package org.raisercostin.jcrawler;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.Metadata;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.nodes.Nodes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Rewriter {

  private final Map<String, PathLocation> globalUrlMap = new HashMap<>();

  public void run(List<PathLocation> inputs, PathLocation output) {
    // 1. Index Phase
    for (PathLocation input : inputs) {
      indexDirectory(input);
    }

    enrichGlobalUrlMapWithDomainRoots();

    // 2. Process Phase
    for (PathLocation input : inputs) {
      processDirectory(input, output);
    }
  }

  private void indexDirectory(PathLocation root) {
    try (Stream<Path> stream = Files.walk(root.toPath())) {
      stream.forEach(path -> {
        PathLocation file = Locations.path(path);
        String filename = path.getFileName().toString();
        if (filename.endsWith(".meta.json")) {
          try {
            String content = file.readContent();
            if (content != null && !content.isEmpty()) {
              Metadata meta = Nodes.json.toObject(content, Metadata.class);
              if (meta.url != null) {
                String actualFilename = filename.substring(0, filename.length() - ".meta.json".length());
                // Jedio might append #meta-- to the filename
                if (actualFilename.endsWith("#meta--")) {
                  actualFilename = actualFilename.substring(0, actualFilename.length() - "#meta--".length());
                }

                PathLocation actualFile = file.parent().get().child(actualFilename);
                globalUrlMap.put(meta.url, actualFile);
                if (meta.url.endsWith("/")) {
                  globalUrlMap.put(meta.url.substring(0, meta.url.length() - 1), actualFile);
                } else {
                  globalUrlMap.put(meta.url + "/", actualFile);
                }
              }
            }
          } catch (Exception e) {
            log.warn("Failed to read metadata from {}", file, e);
          }
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void processDirectory(PathLocation input, PathLocation outputBase) {
    try (Stream<Path> stream = Files.walk(input.toPath())) {
      stream.forEach(path -> {
        if (Files.isDirectory(path))
          return;

        PathLocation file = Locations.path(path);

        Path relativePath = input.toPath().relativize(path);
        String relativeString = relativePath.toString().replace("\\", "/");

        PathLocation dest = outputBase.child(relativeString);

        dest.parent().forEach(p -> p.mkdirIfNeeded());

        String ext = getExtension(path.getFileName().toString());
        if (ext.equals("html") || ext.equals("htm")) {
          localizeHtml(file, dest);
        } else {
          String filename = path.getFileName().toString();
          if (!filename.endsWith(".meta.json") && !filename.endsWith(".links.json") && !filename.endsWith(".tmp2")) {
            file.copyToFile(dest);
          }
        }
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getExtension(String filename) {
    int i = filename.lastIndexOf('.');
    if (i > 0) {
      return filename.substring(i + 1);
    }
    return "";
  }

  private void localizeHtml(PathLocation source, PathLocation dest) {
    try {
      String content = source.readContent();
      Document doc = Jsoup.parse(content);

      String baseUrl = null;
      PathLocation metaFile = source.meta("", ".meta.json");
      if (metaFile.exists()) {
        try {
          Metadata meta = Nodes.json.toObject(metaFile.readContent(), Metadata.class);
          baseUrl = meta.url;
        } catch (Exception e) {
          log.debug("No valid metadata for {}", source);
        }
      }

      rewriteElements(doc, "a", "href", source, baseUrl);
      rewriteElements(doc, "link", "href", source, baseUrl);
      rewriteElements(doc, "img", "src", source, baseUrl);
      rewriteElements(doc, "script", "src", source, baseUrl);

      // Handle srcset attribute for responsive images
      rewriteSrcsetElements(doc, "img", source, baseUrl);
      rewriteSrcsetElements(doc, "source", source, baseUrl);

      // Handle inline scripts and styles
      rewriteScriptContents(doc, source);
      rewriteStyleContents(doc, source);
      rewriteStyleAttributes(doc, source);

      dest.write(doc.outerHtml());
    } catch (Exception e) {
      log.error("Failed to localize {}", source, e);
      source.copyToFile(dest);
    }
  }

  private void rewriteElements(Document doc, String tag, String attr, PathLocation currentSourceFile, String baseUrl) {
    Elements elements = doc.select(tag + "[" + attr + "]");
    for (Element el : elements) {
      String originalLink = el.attr(attr);
      String newLink = transformLink(originalLink, currentSourceFile, baseUrl);
      if (newLink != null && !newLink.equals(originalLink)) {
        el.attr(attr, newLink);
      }
    }
  }

  private void rewriteScriptContents(Document doc, PathLocation currentSourceFile) {
    Elements elements = doc.select("script");
    for (Element el : elements) {
      if (!el.hasAttr("src")) {
        String content = el.data();
        String newContent = replaceUrlsInString(content, currentSourceFile);
        if (!newContent.equals(content)) {
          el.empty().appendChild(new DataNode(newContent));
        }
      }
    }
  }

  private void rewriteStyleContents(Document doc, PathLocation currentSourceFile) {
    Elements elements = doc.select("style");
    for (Element el : elements) {
      String content = el.data();
      String newContent = replaceUrlsInString(content, currentSourceFile);
      if (!newContent.equals(content)) {
        el.empty().appendChild(new DataNode(newContent));
      }
    }
  }

  private void rewriteStyleAttributes(Document doc, PathLocation currentSourceFile) {
    Elements elements = doc.select("[style]");
    for (Element el : elements) {
      String content = el.attr("style");
      String newContent = replaceUrlsInString(content, currentSourceFile);
      if (!newContent.equals(content)) {
        el.attr("style", newContent);
      }
    }
  }

  private String replaceUrlsInString(String content, PathLocation currentSourceFile) {
    if (content == null || content.isEmpty()) {
      return content;
    }
    String[] result = { content };
    List<String> sortedUrls = globalUrlMap.keySet().stream()
        .sorted(Comparator.comparingInt(String::length).reversed())
        .toList();

    for (String url : sortedUrls) {
      if (result[0].contains(url)) {
        PathLocation target = globalUrlMap.get(url);
        String relativePath = calculateRelativePath(currentSourceFile, target);
        result[0] = result[0].replace(url, relativePath);
      }
    }

    return result[0];
  }

  private void enrichGlobalUrlMapWithDomainRoots() {
    Map<String, PathLocation> newEntries = new HashMap<>();
    globalUrlMap.forEach((url, path) -> {
      try {
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host != null) {
          String rootUrl = uri.getScheme() + "://" + host;
          io.vavr.control.Option<PathLocation> domainRoot = findDomainRoot(path, host);

          if (domainRoot.isDefined()) {
            // 1. Standard Root URL: https://example.com
            if (!globalUrlMap.containsKey(rootUrl)) {
              newEntries.put(rootUrl, domainRoot.get());
              log.info("Localizer: Found domain root {} -> {}", rootUrl, domainRoot.get());
            }

            // 2. Escaped JSON URL: https:\/\/example.com
            String escapedUrl = rootUrl.replace("/", "\\/");
            if (!globalUrlMap.containsKey(escapedUrl) && !newEntries.containsKey(escapedUrl)) {
              newEntries.put(escapedUrl, domainRoot.get());
            }

            // 3. Bare Host: example.com (Handle with care, ensure it maps to the root)
            // Only add if not already present and if it's a unique enough string (contains
            // dot)
            if (host.contains(".") && !globalUrlMap.containsKey(host) && !newEntries.containsKey(host)) {
              newEntries.put(host, domainRoot.get());
            }
          }
        }
      } catch (Exception e) {
        // ignore
      }
    });
    globalUrlMap.putAll(newEntries);
  }

  private io.vavr.control.Option<PathLocation> findDomainRoot(PathLocation fromFile, String domainDirectoryName) {
    try {
      io.vavr.control.Option<PathLocation> currentOpt = fromFile.parent();
      while (currentOpt.isDefined()) {
        PathLocation current = currentOpt.get();
        if (current.toPath().getFileName().toString().equals(domainDirectoryName)) {
          return io.vavr.control.Option.of(current);
        }
        currentOpt = current.parent();
      }
    } catch (Exception e) {
      // ignore
    }
    return io.vavr.control.Option.none();
  }

  private void rewriteSrcsetElements(Document doc, String tag, PathLocation currentSourceFile, String baseUrl) {
    Elements elements = doc.select(tag + "[srcset]");
    for (Element el : elements) {
      String originalSrcset = el.attr("srcset");
      String newSrcset = transformSrcset(originalSrcset, currentSourceFile, baseUrl);
      if (newSrcset != null && !newSrcset.equals(originalSrcset)) {
        el.attr("srcset", newSrcset);
      }
    }
  }

  private String transformSrcset(String srcset, PathLocation currentSourceFile, String baseUrl) {
    if (srcset == null || srcset.trim().isEmpty()) {
      return srcset;
    }

    // srcset format: "url1 1x, url2 2x, url3 3x" or "url1 100w, url2 200w"
    // IMPORTANT: URLs can contain commas (e.g., w_945,h_915), so we can't just
    // split on ", "
    // We split on ", " followed by "http" to identify the start of a new URL

    java.util.List<String> entries = new java.util.ArrayList<>();
    int start = 0;
    int pos = 0;

    while (pos < srcset.length()) {
      // Look for ", http://", ", https://", or ", /" pattern (comma-space before next
      // URL)
      int nextHttp = srcset.indexOf(", http://", pos);
      int nextHttps = srcset.indexOf(", https://", pos);
      int nextSlash = srcset.indexOf(", /", pos);
      int nextDelim = -1;

      // Find the earliest delimiter
      if (nextHttp != -1)
        nextDelim = nextHttp;
      if (nextHttps != -1 && (nextDelim == -1 || nextHttps < nextDelim))
        nextDelim = nextHttps;
      if (nextSlash != -1 && (nextDelim == -1 || nextSlash < nextDelim))
        nextDelim = nextSlash;

      if (nextDelim == -1) {
        // Last entry
        entries.add(srcset.substring(start).trim());
        break;
      } else {
        // Found next entry
        entries.add(srcset.substring(start, nextDelim).trim());
        start = nextDelim + 2; // Skip ", "
        pos = start;
      }
    }

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < entries.size(); i++) {
      String entry = entries.get(i).trim();
      if (entry.isEmpty())
        continue;

      // Split URL from size descriptor (e.g., "1x", "2x", "100w")
      // The descriptor is the last whitespace-separated token
      String[] parts = entry.split("\\s+");
      String url;
      String descriptor = "";

      if (parts.length > 1) {
        // Last part is the descriptor (e.g., "1x", "2x", "100w")
        descriptor = parts[parts.length - 1];
        // Everything else is the URL (rejoin in case URL had spaces, though unlikely)
        url = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
      } else {
        url = parts[0];
      }

      // Transform the URL
      String transformedUrl = transformLink(url, currentSourceFile, baseUrl);

      if (i > 0) {
        result.append(", ");
      }
      result.append(transformedUrl);
      if (!descriptor.isEmpty()) {
        result.append(" ").append(descriptor);
      }
    }

    return result.toString();
  }

  private String transformLink(String link, PathLocation currentSourceFile, String baseUrl) {
    if (link == null || link.startsWith("#") || link.startsWith("javascript:") || link.startsWith("mailto:")
        || link.startsWith("tel:")) {
      return link;
    }

    String absoluteUrl = link;
    if (!link.startsWith("http://") && !link.startsWith("https://")) {
      if (baseUrl != null) {
        try {
          absoluteUrl = URI.create(baseUrl).resolve(link).toString();
        } catch (IllegalArgumentException e) {
          // ignore
        }
      }
    }

    // Normalize URL by removing spaces that Jsoup may have added during HTML
    // parsing
    // This is particularly important for srcset URLs where Jsoup adds spaces after
    // commas
    String normalizedUrl = absoluteUrl.replaceAll("\\s+", "");

    if (globalUrlMap.containsKey(normalizedUrl)) {
      PathLocation target = globalUrlMap.get(normalizedUrl);
      return calculateRelativePath(currentSourceFile, target);
    }

    // Also try the original URL in case it was already normalized
    if (!normalizedUrl.equals(absoluteUrl) && globalUrlMap.containsKey(absoluteUrl)) {
      PathLocation target = globalUrlMap.get(absoluteUrl);
      return calculateRelativePath(currentSourceFile, target);
    }

    // Try decoded URL (to handle %7B vs { mismatches)
    try {
      String decodedUrl = java.net.URLDecoder.decode(absoluteUrl, java.nio.charset.StandardCharsets.UTF_8);
      if (!decodedUrl.equals(absoluteUrl) && globalUrlMap.containsKey(decodedUrl)) {
        PathLocation target = globalUrlMap.get(decodedUrl);
        return calculateRelativePath(currentSourceFile, target);
      }
    } catch (Exception e) {
      // ignore
    }

    return link;
  }

  private String calculateRelativePath(PathLocation fromFile, PathLocation toFile) {
    try {
      Path fromPath = fromFile.parent().get().toPath();
      Path toPath = toFile.toPath();
      String rel = fromPath.relativize(toPath).toString();
      return rel.replace("\\", "/");
    } catch (Exception e) {
      log.warn("Error calculating relative path", e);
      return toFile.toExternalForm();
    }
  }
}
