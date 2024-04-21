package org.raisercostin.jcrawl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.jedio.url.WebClientLocation2;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.Metadata;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.nodes.Nodes;
import org.springframework.http.MediaType;

@Slf4j
public class JCrawler {
  @Value
  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
  @Slf4j
  public static class CrawlConfig {
    public static CrawlConfig of(WebLocation webLocation, Option<ReadableFileLocation> whitelistSample) {
      boolean includeQuery = false;
      Option<Set<String>> whitelist = whitelistSample
        .map(x -> extractLinksFromContent(x.readContent(), null, null)
          .map(z -> SimpleUrl.from(z.link).withoutQuery().toExternalForm())
          .toSet());
      return new CrawlConfig(webLocation, webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toSet(),
        includeQuery, whitelist);
    }

    WebLocation webLocation;
    Set<String> children;
    boolean includeQuery;
    Option<Set<String>> whitelist;

    public boolean acceptCrawl(SimpleUrl link) {
      if (whitelist.isEmpty() || whitelist.get().isEmpty()) {
        return true;
      }
      return whitelist.get().contains(link.toExternalForm());
    }
  }

  public static void crawl(WebLocation webLocation, Option<ReadableFileLocation> whitelistSample,
      DirLocation destination) {
    whitelistSample = whitelistSample.filter(x -> x.exists());
    CrawlConfig config = CrawlConfig.of(webLocation, whitelistSample);
    log.info("crawling [{}] to {}", webLocation, destination);
    List<String> files = webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toList();
    files.forEach(System.out::println);
    Set<String> visited = API.Set();
    crawl(config, visited, files.map(url -> HyperLink.of(url)), destination);
    //    files.forEach(
    //      file -> extractLinks(file.copyTo(destination.child(slug(file)).asWritableFile(),
    //        CopyOptions.copyDoNotOverwrite().withDefaultReporting())));
  }

  //TODO - breadth first
  //depth first
  private static Set<String> crawl(CrawlConfig config, Set<String> visited2, Traversable<HyperLink> todo,
      DirLocation destination) {
    return todo.foldLeft(visited2, (visited, link) -> {
      SimpleUrl href = link.link(config.includeQuery);
      if (!visited.contains(href.toExternalForm()) && config.acceptCrawl(href)) {
        return crawl(
          config,
          visited.add(href.toExternalForm()),
          downloadAndExtractLinks(config, link, destination),
          destination);
      } else {
        log.info("already visited or ignored {}", link);
        return visited;
      }
    });
  }

  private static Traversable<HyperLink> downloadAndExtractLinks(CrawlConfig config, HyperLink hyperLink,
      DirLocation destination) {
    SimpleUrl link = hyperLink.link(config.includeQuery);
    if (accept(config, link)) {
      try {
        RequestResponse content = WebClientLocation2.get(link).readCompleteContentSync(null);
        WritableFileLocation dest = destination.child(slug(link)).asWritableFile();
        dest.write(content.getBody());
        ReferenceLocation metaJson = dest.meta("", ".meta.json");
        metaJson.asPathLocation().write(content.computeMetadata());
        //Locations.url(link)
        //        .copyToFileAndReturnIt(dest,
        //          CopyOptions
        //            .copyDoNotOverwriteButIgnore()
        //            .withCopyMeta(true)
        //            .withDefaultReporting())
        return extractLinks(dest, metaJson);
      } catch (Exception e) {
        log.error("couldn't extract links from {}", hyperLink, e);
        return Iterator.empty();
      }
    } else {
      log.info("following not allowed {}", hyperLink);
      return Iterator.empty();
    }
  }

  private static boolean accept(CrawlConfig config, SimpleUrl link) {
    String url = link.toExternalForm();
    return config.children.exists(x -> url.startsWith(x));
  }

  //val notParsedUrls = Seq("javascript", "tel")
  //Extract links from content taking into consideration the base url but also the possible <base> tag attribute.
  //<base href="http://www.cartierbratieni.ro/" />
  private static Traversable<HyperLink> extractLinks(WritableFileLocation source, ReferenceLocation metaJson) {
    ReferenceLocation metaLinks = source.meta("", ".links.json");
    if (!metaLinks.exists()) {
      String metaContent = metaJson.asReadableFile().readContent();
      Metadata meta = Nodes.json.toObject(metaContent, Metadata.class);
      //Option<String> contentType = meta.responseHeaders.getContentType()==MediaType.TEXT_HTML;
      boolean isHtmlAnd200 = meta.responseHeaders.getContentType().getType().equals(MediaType.TEXT_HTML.getType())
          &&
          meta.statusCodeValue == 200;
      log.info("searching links in [{}] from {}", meta.responseHeaders.getContentType(), source);
      String sourceUrl = meta.url;
      //String sourceUrl = meta2.httpMetaRequestUri().get();
      Iterator<HyperLink> result = isHtmlAnd200 ? extractLinks(source, sourceUrl) : Iterator.empty();
      List<HyperLink> all = result.toList();
      int status = meta.statusCodeValue;
      if (300 <= status && status < 400 && meta.responseHeaders.getLocation() != null) {
        //String sourceUrl = meta2.httpMetaRequestUri().get();
        all = all
          .append(
            HyperLink.of(meta.responseHeaders.getLocation().toString(), "Moved Permanently - 301", "", sourceUrl,
              source.toExternalForm()));
      }

      //      YAMLMapper mapper = Nodes.yml.mapper();
      //      mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
      //      mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
      //      mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
      String c = Nodes.json.toString(all);
      metaLinks.asWritableFile().write(c);
      return all;
    } else {
      return Nodes.json.toIterator(metaLinks.asReadableFile().readContent(), HyperLink.class);
    }
  }

  private static Pattern exp(String sep) {
    return Pattern.compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "]*)" + sep + "[^>]*>(.*?)</a>");
    //regular expressions with named group consumes 17% of all time
    //("href", "text")
  }

  private final static Seq<Pattern> allExp = API.Seq(exp("'"), exp("\\\""));

  private static Iterator<HyperLink> extractLinks(WritableFileLocation source, String sourceUrl) {
    val content = source.asReadableFile().readContent();
    return extractLinksFromContent(content, source.toExternalForm(), sourceUrl);
  }

  private static Iterator<HyperLink> extractLinksFromContent(final java.lang.String content,
      String source,
      String sourceUrl) {
    Iterator<HyperLink> result;
    result = allExp.iterator().flatMap(exp -> {
      Iterator<Matcher> all = Iterator.continually(exp.matcher(content)).takeWhile(matcher -> matcher.find());
      return all.map(
        m -> HyperLink.of(m.group(1).trim(), m.group(2).trim(), m.group().trim(), sourceUrl, source));
    });
    return result;
  }

  private static RelativeLocation slug(SimpleUrl file) {
    return slug(file.toExternalForm());
  }

  private static RelativeLocation slug(String url) {
    return Locations.relative(SlugEscape.toSlug(url));
  }
}
