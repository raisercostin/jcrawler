///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//COMPILE_OPTIONS -parameters
//RUNTIME_OPTIONS -Dfile.encoding=UTF-8

// Core dependencies
//DEPS info.picocli:picocli:4.7.5
//DEPS org.slf4j:slf4j-api:2.0.9
//DEPS ch.qos.logback:logback-classic:1.4.11
//DEPS ch.qos.logback:logback-core:1.4.11

// Jackson
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3

// Vavr
//DEPS io.vavr:vavr:0.10.4

// Guava
//DEPS com.google.guava:guava:33.1.0-jre

// Apache Commons
//DEPS org.apache.commons:commons-lang3:3.14.0
//DEPS org.apache.commons:commons-text:1.11.0
//DEPS commons-io:commons-io:2.15.1
//DEPS org.brotli:dec:0.1.2
//DEPS com.github.luben:zstd-jni:1.5.5-11

// Lombok (compile-time + annotation processor) - 1.18.34+ required for Java 23
//DEPS org.projectlombok:lombok:1.18.36
//JAVAC_OPTIONS -processor lombok.launch.AnnotationProcessorHider$AnnotationProcessor

// JSoup
//DEPS org.jsoup:jsoup:1.17.2

// Spring Core
//DEPS org.springframework:spring-core:6.1.3

// Reactor Netty
//DEPS io.projectreactor.netty:reactor-netty-http:1.1.15

// Repositories (order matters - Maven Central first, then custom repos)
//REPOS mavencentral
//REPOS raisercostin=https://raw.githubusercontent.com/raisercostin/maven-repo/master

// Raisercostin libs (from GitHub maven repo)
//DEPS org.raisercostin:jedio:0.102
//DEPS org.raisercostin:jedi-nodes-java:0.34

// Source files
//SOURCES Generators.java
//SOURCES HyperLink.java
//SOURCES ParallelGraphTraverser.java
//SOURCES RichPicocli.java
//SOURCES Slug.java
//SOURCES Rewriter.java
//SOURCES ../../../com/namekis/utils/RichCli.java

package org.raisercostin.jcrawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.brotli.dec.BrotliInputStream;
import com.github.luben.zstd.ZstdInputStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import io.vavr.API;
import io.vavr.collection.Array;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringTokenizer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.RichThrowable;
import org.jedio.struct.RichIterable;
import org.raisercostin.jcrawler.RichPicocli.LocationConverter;
import org.raisercostin.jcrawler.RichPicocli.PicocliDir;
import org.raisercostin.jcrawler.RichPicocli.VavrConverter;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.Metadata;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.WebClientFactory;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.nodes.JacksonNodes;
import org.raisercostin.nodes.Nodes;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import reactor.netty.http.HttpProtocol;
import com.namekis.utils.RichCli;

@AllArgsConstructor
@ToString
@With
@Command(name = "jcrawl", mixinStandardHelpOptions = true, version = "jcrawl 0.1", description = "Crawler tool.",
    // subcommands = GenerateCompletion.class,
    usageHelpAutoWidth = true, usageHelpWidth = 120, showDefaultValues = true, versionProvider = JCrawler.MyVersionProvider.class)
@Slf4j
public class JCrawler implements Callable<Integer> {
  /**
   * Standard options inherited from RichCli.BaseOptions providing:
   * -v/--verbose, -q/--quiet, --color, --debug, --trace, --workdir
   */
  static class StandardOptions extends RichCli.BaseOptions {
    // Inherits all standard options from BaseOptions:
    // -v/--verbose: Increase verbosity (use multiple -vvv)
    // -q/--quiet: Suppress log output (use multiple -qqq)
    // --color/--no-color: Enable/disable colored output
    // --debug: Enable debug mode with detailed logs
    // --trace: Show full stack traces for errors
    // --workdir: Base directory for operations
  }

  public final java.util.Set<String> ignoredExternalDomains = java.util.concurrent.ConcurrentHashMap.newKeySet();

  @ArgGroup(exclusive = false, heading = "Development options:%n", order = 100)
  StandardOptions standardOpts = new StandardOptions();

  static class PathLocationConverter implements picocli.CommandLine.ITypeConverter<PathLocation> {
    @Override
    public PathLocation convert(String value) throws Exception {
      return Locations.path(value);
    }
  }

  static class RewriteOptions {
    @picocli.CommandLine.Option(names = "--rewrite-inputs", split = ",", description = "Directories to rewrite")
    java.util.List<String> inputs;

    @picocli.CommandLine.Option(names = "--rewrite-output", description = "Output directory for rewrite")
    String output;
  }

  @ArgGroup(exclusive = false, heading = "Rewrite options:%n", order = 200)
  RewriteOptions rewriteOpts = new RewriteOptions();

  public static void main(String[] args) {
    // Use RichCli for mature logging configuration and error handling
    RichCli.main(args, () -> new JCrawler());
  }

  public static void mainOne(String args, boolean exitAtEnd) {
    main(split(args), exitAtEnd);
  }

  public static void main(String[] args, boolean exitAtEnd) {
    // Use RichCli for mature logging configuration and error handling
    RichCli.main(args, () -> new JCrawler(), exitAtEnd);
  }

  private static String[] split(String cmdWithSpaces) {
    StringTokenizer tokenizer = new StringTokenizer(cmdWithSpaces, ' ', '"');
    tokenizer.setIgnoreEmptyTokens(true);
    return tokenizer.getTokenArray();
  }

  // git.branch=master
  // ... (keeping unchanged lines for context matching if needed, but
  // replace_file_content replaces range)
  // ...

  // Skipping unrelated parts down to call() methods...
  // I need to be careful with replace_file_content since I can't skip large
  // chunks in ReplacementContent.
  // I will split this into multiple chunks using multi_replace_file_content to be
  // safer.

  // git.branch=master
  // git.build.host=DESKTOP-HO5N784
  // git.build.time=2024-06-12T20\:53\:44+0300
  // git.build.user.email=raisercostin@gmail.com
  // git.build.user.name=raisercostin
  // git.build.version=0.3-SNAPSHOT
  // git.closest.tag.commit.count=6
  // git.closest.tag.name=jcrawler-0.2
  // git.commit.id=3454d8ad006219cdd9a49f930a31975fab87b334
  // git.commit.id.abbrev=3454d8a
  // git.commit.id.describe=jcrawler-0.2-6-g3454d8a
  // git.commit.id.describe-short=jcrawler-0.2-6
  // git.commit.message.full=upgraded
  // git.commit.message.short=upgraded
  // git.commit.time=2024-06-12T20\:49\:24+0300
  // git.commit.user.email=raisercostin@gmail.com
  // git.commit.user.name=raisercostin
  // git.dirty=false
  // git.local.branch.ahead=0
  // git.local.branch.behind=0
  // git.remote.origin.url=https\://github.com/raisercostin/jcrawler.git
  // git.tags=
  // git.total.commit.count=94
  @JsonNaming(PropertyNamingStrategies.LowerDotCaseStrategy.class)
  public static class GitInfo {
    public String gitBranch;
    public String gitBuildHost;
    public String gitBuildTime;
    public String gitBuildUserEmail;
    public String gitBuildUserName;
    public String gitBuildVersion;
    public int gitClosestTagCommitCount;
    public String gitClosestTagName;
    public String gitCommitId;
    public String gitCommitIdAbbrev;
    public String gitCommitIdDescribe;
    @JsonProperty("git.commit.id.describe-short")
    public String gitCommitIdDescribeShort;
    public String gitCommitMessageFull;
    public String gitCommitMessageShort;
    public String gitCommitTime;
    public String gitCommitUserEmail;
    public String gitCommitUserName;
    public boolean gitDirty;
    public int gitLocalBranchAhead;
    public int gitLocalBranchBehind;
    public String gitRemoteOriginUrl;
    public String gitTags;
    public int gitTotalCommitCount;
    //
    // @JsonAnyGetter
    // private Map<String, Object> otherProperties = new HashMap<>();
    //
    // @JsonAnySetter
    // public void setOtherProperty(String name, Object value) {
    // otherProperties.put(name, value);
    // }
  }

  static class MyVersionProvider implements IVersionProvider {
    public static Properties loadProperties(String filePath) throws Exception {
      Properties properties = new Properties();
      try (InputStream input = MyVersionProvider.class.getClassLoader().getResourceAsStream(filePath)) {
        if (input == null) {
          throw new RuntimeException("Unable to find " + filePath);
        }
        properties.load(input);
      }
      return properties;
    }

    @Override
    public String[] getVersion() throws Exception {
      Properties properties = loadProperties("git.properties");
      ObjectMapper mapper = new ObjectMapper();
      GitInfo git = mapper.convertValue(properties, GitInfo.class);
      // GitInfo git = Nodes.hocon.toObject(
      // Locations.classpath("git.properties").readContent(),
      // GitInfo.class);
      // Return the version dynamically
      String version = """
          %s
          Build Time: %s
          Build User: %s (%s)""".formatted(
          git.gitCommitIdDescribe,
          git.gitBuildTime,
          git.gitBuildUserName,
          git.gitBuildUserEmail);

      return new String[] { version };
    }
  }

  public static JCrawler crawler() {
    return new JCrawler();
  }

  public static JCrawler of(WebLocation webLocation, DirLocation cache,
      Option<ReadableFileLocation> whitelistSample) {
    Set<String> whitelist2 = whitelistSample
        .map(x -> extractLinksFromContent(0, x.readContent(), null, null)
            .map(z -> z.withoutQuery())
            .toSet())
        .getOrNull();
    Seq<String> urls = webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toList();
    return of(cache, urls).withAdditionalAccepts(whitelist2);
  }

  private static JCrawler of(DirLocation projectDir, Seq<String> urls) {
    JCrawler crawler = new JCrawler(new StandardOptions(), new RewriteOptions(), null, TraversalType.BREADTH_FIRST,
        Nodes.json, false,
        new PicocliDir(projectDir),
        -1, 3, null, Duration.ofDays(100), null, null, 100, false, null, ContentStorage.decompressed, null, true)
        .withUrlsAndAccept(urls);
    return crawler;
  }

  @AllArgsConstructor
  public static class LinkMatcher {
    public final Pattern pattern;
    public final boolean hasUrl;
    public final boolean hasSrcSet;
    public final boolean hasDirective;
    public final boolean isResource;
  }

  private static LinkMatcher exp(String sep) {
    Pattern pattern = Pattern.compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "(?<url>[^" + sep + "#]*)(#[^" + sep + "]*)?"
        + sep + "[^>]*>(?<text>.*?)</a>");
    return new LinkMatcher(pattern, true, false, false, false);
  }

  // regular expressions with named group consumes 17% of all time
  // ("href", "text")
  private static LinkMatcher urlInStyleExp() {
    Pattern pattern = Pattern.compile("(?i)url\\(['\"]?(?<url>[^'\")]+)['\"]?\\)");
    return new LinkMatcher(pattern, true, false, false, true);
  }

  private static LinkMatcher linkTagExp(String sep) {
    Pattern pattern = Pattern.compile("(?i)<link[^>]*\\s+href=" + sep + "(?<url>[^" + sep + "]*)" + sep + "[^>]*>");
    return new LinkMatcher(pattern, true, false, false, true);
  }

  private static LinkMatcher robotsTxtExp() {
    Pattern pattern = Pattern.compile("(?i)(?<directive>Sitemap|Allow|Disallow):\\s*(?<url>[^\\s]+)");
    return new LinkMatcher(pattern, true, false, true, true);
  }

  private static LinkMatcher imgExp(String sep) {
    Pattern pattern = Pattern
        .compile("(?i)(?s)<img[^>]*\\s+src\\b\\s*=" + sep + "(?<url>[^" + sep + "]*)" + sep + "[^>]*>");
    return new LinkMatcher(pattern, true, false, false, true);
  }

  private static LinkMatcher srcsetExp(String tag, String sep) {
    Pattern pattern = Pattern
        .compile("(?i)(?s)<" + tag + "[^>]*\\s+srcset\\b\\s*=" + sep + "(?<srcset>[^" + sep + "]*)" + sep + "[^>]*>");
    // LinkMatcher(pattern, hasUrl, hasSrcSet, isRedirect, isResource)
    // We set dummy url group if needed, or null
    return new LinkMatcher(pattern, false, true, false, true);
  }

  // script src=
  private static LinkMatcher scriptSrc(String sep) {
    String tag = "script";
    String attribute = "src";
    Pattern pattern = Pattern
        .compile("(?i)(?s)<" + tag + "[^>]*\\s+" + attribute + "\\b\\s*=" + sep + "(?<url>[^" + sep + "]*)" + sep);
    return new LinkMatcher(pattern, true, false, false, true);
  }

  private static LinkMatcher xmlStylesheetExp() {
    Pattern pattern = Pattern.compile("(?i)<\\?xml-stylesheet[^>]*\\s+href\\b\\s*=['\"](?<url>[^'\"]+)['\"][^>]*\\?>");
    return new LinkMatcher(pattern, true, false, false, true);
  }

  private static LinkMatcher sitemapLocExp() {
    Pattern pattern = Pattern.compile("(?i)<loc>(?<url>[^<]+)</loc>");
    return new LinkMatcher(pattern, true, false, false, true);
  }

  // Define all patterns using the LinkMatcher class
  private static final List<LinkMatcher> allLinkMatchers = List.of(
      exp("'"),
      exp("\\\""),
      imgExp("'"),
      imgExp("\\\""),
      srcsetExp("img", "'"),
      srcsetExp("img", "\\\""),
      srcsetExp("source", "'"),
      srcsetExp("source", "\\\""),
      urlInStyleExp(),
      linkTagExp("'"),
      linkTagExp("\\\""),
      robotsTxtExp(),
      xmlStylesheetExp(),
      sitemapLocExp(),
      scriptSrc("\\\""),
      scriptSrc("'"));

  // The extractor logic
  static io.vavr.collection.Iterator<HyperLink> extractLinksFromContent(int depth, final String content,
      String source, String sourceUrl) {
    if (log.isDebugEnabled()) {
      log.debug("extractLinksFromContent depth={} sourceUrl={} contentLength={}", depth, sourceUrl, content.length());
    }
    io.vavr.collection.Iterator<HyperLink> result;
    result = allLinkMatchers.iterator().flatMap(linkMatcher -> {
      Matcher matcher = linkMatcher.pattern.matcher(content);
      io.vavr.collection.Iterator<Matcher> allMatches = io.vavr.collection.Iterator.continually(matcher)
          .takeWhile(m -> m.find());

      return allMatches.flatMap(m -> {
        String url = linkMatcher.hasUrl ? m.group("url") != null ? m.group("url").trim() : null : null;
        String directive = linkMatcher.hasDirective ? m.group("directive") != null ? m.group("directive").trim() : ""
            : null;
        String srcset = linkMatcher.hasSrcSet && m.group("srcset") != null ? m.group("srcset").trim() : "";

        if (url == null && srcset.isEmpty()) {
          return io.vavr.collection.List.<HyperLink>empty().iterator();
        }

        // Create iterator for src and all srcset entries
        boolean isResource = linkMatcher.isResource;
        if (!isResource && url != null) {
          String lower = url.toLowerCase();
          if (lower.endsWith(".pdf") || lower.endsWith(".zip") || lower.endsWith(".mp3") || lower.endsWith(".avi")
              || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".css") || lower.endsWith(".js")) {
            isResource = true;
          }
        }
        io.vavr.collection.List<HyperLink> links = io.vavr.collection.List.empty();
        if (url != null) {
          // Skip data URIs BEFORE decoding
          // Also skip corrupted versions like data\uF03A (result of Slug transformation)
          String lowerUrl = url.toLowerCase();
          if (lowerUrl.startsWith("data:") || lowerUrl.startsWith("data\uF03A") || lowerUrl.startsWith("dataimage/")) {
            log.debug("Skipping data URI: {}", url.substring(0, Math.min(50, url.length())));
            return io.vavr.collection.List.<HyperLink>empty().iterator();
          }

          // Skip template variables BEFORE decoding (e.g., ${i.uri} or encoded
          // $%7Bi.uri%7D)
          if ((url.contains("${") && url.contains("}")) || (url.contains("$%7B") && url.contains("%7D"))) {
            log.debug("Skipping template variable URL: {}", url);
            return io.vavr.collection.List.<HyperLink>empty().iterator();
          }

          // Skip extremely long URLs that are likely dynamic model/Wix APIs causing
          // validation failures
          if (url.length() > 2000) {
            log.debug("Skipping extremely long URL (length={}): {}", url.length(), url.substring(0, 100));
            return io.vavr.collection.List.<HyperLink>empty().iterator();
          }

          // Decode URL to handle double-encoded URLs (e.g., %257B -> %7B)
          try {
            String decodedUrl = java.net.URLDecoder.decode(url, java.nio.charset.StandardCharsets.UTF_8).trim();

            if (!decodedUrl.equals(url)) {
              // Skip decoded data URIs (e.g., data%3Aimage -> data:image)
              String lowerDecoded = decodedUrl.toLowerCase();
              if (lowerDecoded.startsWith("data:") || lowerDecoded.startsWith("data\uF03A")
                  || lowerDecoded.startsWith("dataimage/")) {
                log.debug("Skipping decoded data URI: {}", decodedUrl.substring(0, Math.min(50, decodedUrl.length())));
                return io.vavr.collection.List.<HyperLink>empty().iterator();
              }

              // Skip template variables after decoding (e.g., $%7Bi.uri%7D -> ${i.uri})
              if (decodedUrl.contains("${") && decodedUrl.contains("}")) {
                log.debug("Skipping decoded template variable URL: {}", decodedUrl);
                return io.vavr.collection.List.<HyperLink>empty().iterator();
              }

              // Use decoded URL (this handles double-encoding)
              url = decodedUrl;
              log.debug("Decoded URL: {}", url.substring(0, Math.min(100, url.length())));
            }
          } catch (Exception e) {
            log.debug("Failed to decode URL, using original: {}", e.getMessage());
          }

          links = links.append(HyperLink.of(url, depth, directive != null ? directive : "", null, m.group().trim(),
              sourceUrl, source, false, isResource));
        }

        if (!srcset.isEmpty()) {
          // Heuristic Splitter:
          // 1. Split if comma is preceded by a descriptor (e.g., " 1x", " 200w").
          // Pattern: (?<=\s\d{1,5}[wx])\s*,\s+
          // 2. Split if comma is followed by an absolute URL or root path.
          // Pattern: ,\s+(?=(?:https?://|/))

          String splitRegex = "(?<=\\s\\d{1,5}(?:\\.\\d+)?[wx])\\s*,\\s+|" +
              ",\\s+(?=(?:https?://|/))";
          Pattern splitPattern = Pattern.compile(splitRegex);
          Matcher splitMatcher = splitPattern.matcher(srcset);
          io.vavr.collection.List<String> srcsetEntries = io.vavr.collection.List.empty();

          int lastEnd = 0;
          while (splitMatcher.find()) {
            srcsetEntries = srcsetEntries.append(srcset.substring(lastEnd, splitMatcher.start()));
            lastEnd = splitMatcher.end();
          }
          // Add the final entry
          srcsetEntries = srcsetEntries.append(srcset.substring(lastEnd));

          for (String entry : srcsetEntries) {
            entry = entry.trim();
            if (entry.isEmpty()) {
              continue;
            }

            // Split by last space to separate URL from descriptor (e.g., "1x", "2x",
            // "100w")
            int lastSpace = entry.lastIndexOf(' ');
            if (lastSpace > 0) {
              String srcsetUrl = entry.substring(0, lastSpace).trim();
              String descriptor = entry.substring(lastSpace + 1).trim();

              // Remove spaces that Jsoup added within the URL (e.g., "w_263, h_189" ->
              // "w_263,h_189")
              srcsetUrl = srcsetUrl.replaceAll("\\s+", "");

              // Skip data URIs in srcset
              if (srcsetUrl.toLowerCase().startsWith("data:")) {
                log.debug("Skipping data URI in srcset: {}", srcsetUrl.substring(0, Math.min(50, srcsetUrl.length())));
                continue;
              }

              links = links.append(HyperLink.of(srcsetUrl, depth, descriptor, null, m.group().trim(), sourceUrl, source,
                  false, isResource));
            }
          }
        }

        return links.iterator();
      });
    });
    return result;
  }

  public enum TraversalType {
    PARALLEL_BREADTH_FIRST {
      @Override
      public <N> Iterable<N> traverse(JCrawler config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return new ParallelGraphTraverser<>(config.maxConnections, successor).startTraversal(todo, config.maxDocs);
      }
    },
    BREADTH_FIRST {
      @Override
      public <N> Iterable<N> traverse(JCrawler config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        // TODO bug in guava traversal that checks all initial links twice
        return Traverser.forGraph(successor).breadthFirst(todo);
      }
    },
    DEPTH_FIRST_PREORDER {
      @Override
      public <N> Iterable<N> traverse(JCrawler config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return Traverser.forGraph(successor).depthFirstPreOrder(todo);
      }
    },
    DEPTH_FIRST_POSTORDER {
      @Override
      public <N> Iterable<N> traverse(JCrawler config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return Traverser.forGraph(successor).depthFirstPostOrder(todo);
      }
    };

    abstract <N> Iterable<N> traverse(JCrawler config, Iterable<N> todo, SuccessorsFunction<N> successor);
  }

  @Spec
  @JsonIgnore
  private CommandSpec spec; // injected by picocli
  @picocli.CommandLine.Option(names = { "-t",
      "--traversal" }, description = "Set the traversal mode: ${COMPLETION-CANDIDATES}.")
  public TraversalType traversalType = TraversalType.PARALLEL_BREADTH_FIRST;
  @JsonIgnore
  public JacksonNodes linksNodes = Nodes.json;
  @picocli.CommandLine.Option(names = { "--recomputeLinks" }, description = "Extract links again from content")
  public boolean recomputeLinks = false;

  @picocli.CommandLine.Option(names = { "-p",
      "--project" }, description = "Project dir for config and crawled content.", converter = LocationConverter.class)
  public PicocliDir projectDir = new PicocliDir(Locations.current().child(".jcrawler"));
  @picocli.CommandLine.Option(names = { "-d", "--maxDocs" })
  public int maxDocs = 10_000;
  @picocli.CommandLine.Option(names = { "-c", "--maxConnections" })
  public int maxConnections = 3;
  @picocli.CommandLine.Option(names = { "--protocol" }, description = "Set the protocol: ${COMPLETION-CANDIDATES}.")
  public HttpProtocol[] protocols = { HttpProtocol.H2, HttpProtocol.HTTP11 };
  @picocli.CommandLine.Option(names = {
      "--expire" }, description = "Expiration as a iso 8601 format like P1DT1S. \n Full format P(n)Y(n)M(n)DT(n)H(n)M(n)S\nSee more at https://www.digi.com/resources/documentation/digidocs/90001488-13/reference/r_iso_8601_duration_format.htm")
  public Duration cacheExpiryDuration = Duration.ofDays(100);
  @picocli.CommandLine.Parameters(paramLabel = "urls", description = """
      Urls to crawl. If urls contain expressions all combinations of that values will be generated:
      - ranges like {start-end}
      - alternatives like {option1|option2|option3}

      For example https://namekis.com/{docA|doc2}/{1-3} will generate the following urls:
      - https://namekis.com/docA/1
      - https://namekis.com/docA/2
      - https://namekis.com/docA/3
      - https://namekis.com/doc2/1
      - https://namekis.com/doc2/2
      - https://namekis.com/doc2/3""", converter = VavrConverter.class, arity = "0..*")
  @With(value = AccessLevel.PRIVATE)
  public Seq<String> urls;
  @picocli.CommandLine.Option(names = { "--accept" }, description = "Additional urls to accept.")
  public java.util.List<String> accept;
  @picocli.CommandLine.Option(names = { "-l",
      "--level" }, description = "Limit depth crawling. Given start urls are level 0. All different links from it are level 1.")
  public int depth = 100;
  @picocli.CommandLine.Option(names = { "--show-stacktrace" }, description = "Show stack trace on errors")
  public boolean debug = false;
  @picocli.CommandLine.Option(names = { "--acceptHostname" }, description = "Template to accept urls with this prefix.")
  public String acceptHostname = "{http|https}://{www.|}%s";

  public enum ContentStorage {
    decompressed,
    compressed,
    both
  }

  @picocli.CommandLine.Option(names = {
      "--content-storage" }, description = "How to store content: ${COMPLETION-CANDIDATES}.")
  public ContentStorage contentStorage = ContentStorage.decompressed;
  public String crawlFormat = "";
  @picocli.CommandLine.Option(names = { "--migrate" }, description = "Migrate takes time to re-read metadata")
  public boolean migrate = true;

  private JCrawler() {
  }

  @Override
  public Integer call() throws Exception {
    if (rewriteOpts.inputs != null && !rewriteOpts.inputs.isEmpty()) {
      if (rewriteOpts.output == null) {
        System.err.println("Error: --rewrite-output is required when --rewrite-inputs is specified.");
        return 1;
      }
      new Rewriter().run(rewriteOpts.inputs.stream().map(x -> Locations.path(x)).toList(),
          Locations.path(rewriteOpts.output));
      return 0;
    }
    // CommandLine.Help help = new CommandLine.Help(spec);
    // System.out.println(help.optionList());
    // crawlIterator().forEach(x -> System.out.println(x.externalForm + " => " +
    // x.localCache));
    var result = crawlIterator().toList();
    result.forEach(x -> log.debug("{} => {}", x.externalForm, x.localCache));

    if (!ignoredExternalDomains.isEmpty()) {
      System.out.println("\n--------------------------------------------------------------");
      System.out.println("Suggestion: The following external domains were encountered:");
      ignoredExternalDomains.stream().sorted().forEach(domain -> System.out.println("  - " + domain));
      System.out.println("\nTo include them in the crawl, add the following arguments:");

      String acceptArgs = ignoredExternalDomains.stream()
          .sorted()
          .map(domain -> "--accept " + domain)
          .collect(java.util.stream.Collectors.joining(" "));
      System.out.println(acceptArgs);
      System.out.println("--------------------------------------------------------------\n");
    }

    // Write full result to .crawl-result.yaml
    java.util.Map<String, Object> finalResult = new java.util.LinkedHashMap<>();
    finalResult.put("config", this);
    finalResult.put("ignoredExternalDomains", ignoredExternalDomains);
    finalResult.put("stats", java.util.Map.of("totalLinks", result.size()));

    projectDir.dir.child(".crawl-result.yaml")
        .asWritableFile()
        .write(Nodes.yml.toString(finalResult));

    return 0;
  }

  public RichIterable<HyperLink> crawlIterator() {
    log.debug("JCrawler started with config:\n{}", Nodes.yml.toString(this));
    CrawlerWorker worker = worker();
    // projectDir.dir.child(".crawl-config.yaml")
    // .asWritableFile()
    // .nonExistingOrElse(x -> x.delete(DeleteOptions.deleteDefault()))
    // // .deleteFile(((SimpleDeleteOptions)
    // // DeleteOptions.deleteDefault()).withIgnoreNonExisting(true))
    // .asWritableFile()
    // .write(Nodes.yml.toString(worker));
    return worker.crawl(
        urls.flatMap(generator -> Generators.parse(generator).generate()).map(x -> HyperLink.of(x)));
  }

  public CrawlerWorker worker() {
    return new CrawlerWorker(this);
  }

  public static RichIterable<HyperLink> crawl(WebLocation webLocation, Option<ReadableFileLocation> whitelistSample,
      DirLocation cache) {
    whitelistSample = whitelistSample.filter(x -> x.exists());
    JCrawler config = JCrawler.of(webLocation, cache, whitelistSample);
    log.info("crawling [{}] to {}", webLocation, cache);
    config.urls.forEach(System.out::println);
    return config.crawlIterator();
  }

  public WritableFileLocation findOldFile(HyperLink href) {
    RichIterable<Slug> slugs = Slug.slugs(href.externalForm);
    return (WritableFileLocation) slugs.map(slug -> cached(slug)).find(file -> file.exists()).get();
  }

  private ReferenceLocation cached(Slug slug) {
    return projectDir.dir.child(slug.slug);
  }

  public ReferenceLocation cached(HyperLink href) {
    Slug slug = href.slug();
    return cached(slug);
  }

  public FileLocation cachedFile(String url) {
    Slug slug = Slug.slugs(url).head();
    return (FileLocation) cached(slug);
  }

  public FileLocation slug(HyperLink href) {
    return cachedFile(href.externalForm);
  }

  public JCrawler withFiltersByPrefix(String... filters) {
    return withAccept(java.util.Arrays.asList(filters));
  }

  public JCrawler withVerbosity(int level) {
    RichCli.setLoggingLevel("org.raisercostin.jcrawler", level);
    return this;
  }

  public JCrawler withProtocol(HttpProtocol... protocols) {
    return withProtocols(protocols);
  }

  public boolean forceDownload(HyperLink href, WritableFileLocation dest) {
    if (cacheExpiryDuration == null) {
      return true;
    }
    // Check for both .html and .html.gz existence when checking modification time
    ReferenceLocation metaJson = dest.meta("", ".meta.json");
    if (metaJson.exists()) {
      try {
        Metadata m = Nodes.json.toObject(metaJson.asReadableFile().readContent(), Metadata.class);
        String encoding = getEncoding(m);
        String ext = getExtensionForEncoding(encoding);
        ReferenceLocation physical = ext.isEmpty() ? dest : dest.parentRef().get().child(dest.filename() + ext);
        if (physical.exists()) {
          return physical.modifiedDateTime().toInstant().isBefore(Instant.now().minus(cacheExpiryDuration));
        }
      } catch (Exception e) {
        log.warn("Error reading metadata for forceDownload check: {}. Enable trace for full stacktrace.", e.toString());
        log.trace("Error reading metadata for forceDownload check", e);
        // Fallback to basic check below
      }
    }
    if (!dest.exists()) {
      // If no metadata and no file, force download
      return true;
    }
    return dest.modifiedDateTime().toInstant().isBefore(Instant.now().minus(cacheExpiryDuration));
  }

  public static InputStream decompressStream(String encoding, InputStream in) throws IOException {
    if ("gzip".equalsIgnoreCase(encoding)) {
      return new java.util.zip.GZIPInputStream(in);
    } else if ("deflate".equalsIgnoreCase(encoding)) {
      return new java.util.zip.InflaterInputStream(in);
    } else if ("br".equalsIgnoreCase(encoding)) {
      return new BrotliInputStream(in);
    } else if ("zstd".equalsIgnoreCase(encoding)) {
      return new ZstdInputStream(in);
    }
    return in;
  }

  public static String getExtensionForEncoding(String encoding) {
    if ("gzip".equalsIgnoreCase(encoding))
      return ".gz";
    if ("br".equalsIgnoreCase(encoding))
      return ".br";
    if ("zstd".equalsIgnoreCase(encoding))
      return ".zst";
    return "";
  }

  public static String getEncodingFromExtension(String ext) {
    if (".gz".equalsIgnoreCase(ext))
      return "gzip";
    if (".br".equalsIgnoreCase(ext))
      return "br";
    if (".zst".equalsIgnoreCase(ext))
      return "zstd";
    return "";
  }

  public static boolean isSame(ReferenceLocation loc1, ReferenceLocation loc2) {
    if (loc1 == null || loc2 == null)
      return false;
    return loc1.absoluteAndNormalized().equalsIgnoreCase(loc2.absoluteAndNormalized());
  }

  public static String getEncoding(Metadata meta) {
    if (meta == null || meta.responseHeaders == null)
      return null;
    return meta.responseHeaders.getFirst("Content-Encoding");
  }

  public JCrawler withProjectPath(PathLocation dir) {
    return withProjectDir(new PicocliDir(dir));
  }

  public JCrawler withProjectPath(String dir) {
    return withProjectDir(new PicocliDir(Locations.path(dir)));
  }

  public JCrawler withUrl(String... urls) {
    return withUrlsAndAccept(Array.of(urls));
  }

  public JCrawler withUrlsAndAccept(Seq<String> urls) {
    return withUrls(urls);
  }

  private JCrawler withAdditionalAccepts(Iterable<String> additionalAccept) {
    if (this.accept == null) {
      java.util.List<String> newAccept = new java.util.ArrayList<>();
      additionalAccept.forEach(newAccept::add);
      return withAccept(newAccept);
    }
    java.util.List<String> newAccept = new java.util.ArrayList<>(this.accept);
    additionalAccept.forEach(newAccept::add);
    return withAccept(newAccept);
  }

  public static class CrawlerWorker {
    public final JCrawler config;
    @JsonIgnore
    public final WebClientFactory client;
    // public final java.util.concurrent.Semaphore semaphore;
    @JsonIgnore
    public final BlockingQueue<String> tokenQueue;
    @JsonIgnore
    public final Cache<String, String> failingServers = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
    private Set<String> accept;

    public CrawlerWorker(JCrawler config) {
      // config.verbosity.configureLoggingLevel();
      this.config = config;
      this.accept = config.urls != null ? config.urls.iterator()
          .map(
              x -> config.acceptHostname.formatted(HyperLink.of(x).hostnameForAccept()))
          .toSet()
          .toSet()
          .addAll(config.accept != null ? io.vavr.collection.List.ofAll(config.accept).map(acc -> {
            if (acc.startsWith("http") || acc.contains("{")) {
              return acc;
            }
            return config.acceptHostname.formatted(acc);
          }) : API.Set())
          .flatMap(x -> Generators.generate(x)) : API.Set();
      this.client = new WebClientFactory("jcrawler", config.protocols);
      // this.semaphore = new Semaphore(config.maxConnections);
      this.tokenQueue = new ArrayBlockingQueue<>(config.maxConnections);

      // Populate the queue with tokens
      for (int i = 0; i < config.maxConnections; i++) {
        tokenQueue.add("c" + i);
      }
      log.debug("Accepts:\n - {}", accept.mkString("\n - "));
    }

    private RichIterable<HyperLink> crawl(Traversable<HyperLink> todo) {
      Iterable<HyperLink> all = config.traversalType.traverse(config, todo, this::downloadAndExtractLinks);
      return RichIterable.ofAll(all)
          .take(config.maxDocs);
    }

    // Visible for testing
    boolean accept(HyperLink link) {
      String url = link.externalForm.toLowerCase();
      for (String protocol : UNSUPPORTED_PROTOCOLS) {
        if (url.startsWith(protocol)) {
          return false;
        }
      }
      boolean accept = accept2(link);
      if (!accept) {
        if (link.depth <= config.depth) {
          boolean unsupported = false;
          for (String protocol : UNSUPPORTED_PROTOCOLS) {
            if (url.startsWith(protocol)) {
              unsupported = true;
              break;
            }
          }
          if (!unsupported) {
            String host = link.hostname();
            if (host != null && !host.isEmpty()) {
              config.ignoredExternalDomains.add(host);
            }
          }
        }
        if (link.isRedirect) {
          log.warn(
              "Redirect target ignored by accept filter: [{}]. If this content is needed, add the domain to --accept.",
              link.externalForm);
        }
      }
      log.debug("{} [{}]", accept ? "accept" : "ignore", link.externalForm);
      return accept;
    }

    private static final java.util.Set<String> UNSUPPORTED_PROTOCOLS = java.util.Set.of(
        "tel:", "mailto:", "javascript:", "data:", "blob:", "file:", "ftp:", "ssh:", "git:");

    private boolean accept2(HyperLink link) {
      if (link.depth > config.depth) {
        return false;
      }
      // Requisites/Resources (images, scripts, etc.) are always accepted to ensure
      // the page renders correctly
      // This mimics "page requisites" behavior of wget/browsers
      if (link.isResource) {
        return true;
      }
      // Use implicit ownership: if we accepted the source link (which lead to this
      // redirect), we should accept the target.
      // E.g. internal link -> 301 -> external link.
      if (link.isRedirect) {
        return true;
      }
      if (accept == null) {
        return false;
      }
      // Early filter for unsupported protocols
      String url = link.externalForm.toLowerCase();
      for (String protocol : UNSUPPORTED_PROTOCOLS) {
        if (url.startsWith(protocol)) {
          return false;
        }
      }
      return accept.exists(x -> link.externalForm.startsWith(x));
    }

    interface A<T extends A<T>> {
      T process(T input);
    }

    static class B implements A<B> {
      @Override
      public B process(B input) {
        // Implementation for B
        return input;
      }
    }

    static class C implements A<C> {
      @Override
      public C process(C input) {
        // Implementation for C
        return input;
      }
    }

    @SneakyThrows
    private Traversable<HyperLink> downloadAndExtractLinks(HyperLink href) {
      if (!accept(href)) {
        return API.Seq();
      }
      String hostname = href.hostname();
      if (hostname != null && failingServers.getIfPresent(hostname) != null) {
        log.debug("ignored failing server for a while [{}]", href.externalForm);
        return API.Seq();
      }
      Metadata metadata = null;
      var contentUid = config.cached(Slug.contentUid(href.externalForm));
      contentUid = contentUid.parentRef().get().child(".index").child(contentUid.filename());
      var destInitial = config.cached(Slug.contentPathInitial(href.externalForm)).asWritableFile();
      WritableFileLocation dest = destInitial;

      var destFromSymlink = contentUid.existingRef().map(x -> x.userSymlinkTarget());
      var metaJson2 = destFromSymlink.map(x -> x.meta("", ".meta.json"));

      // Check for existence considering compressed variant too
      boolean destExists = destFromSymlink.map(x -> {
        if (x.exists())
          return true;
        // Use metadata to check for compressed existence
        ReferenceLocation mj = x.meta("", ".meta.json");
        if (mj.exists()) {
          try {
            Metadata m = Nodes.json.toObject(mj.asReadableFile().readContent(), Metadata.class);
            String ext = getExtensionForEncoding(getEncoding(m));
            return !ext.isEmpty() && x.parentRef().get().child(x.filename() + ext).exists();
          } catch (Exception e) {
            log.debug("Ignored error checking metadata existence for {}: {}. Enable trace for details.", mj,
                e.toString());
            log.trace("Ignored error checking metadata existence", e);
            return false;
          }
        }
        return false;
      }).getOrElse(false);
      boolean exists = contentUid.exists() && /* contentUid.isSymlink() && */ destExists
          && metaJson2.get().exists();
      // WritableFileLocation old = config.findOldFile(href);
      // WritableFileLocation dest = config.cached(href).asWritableFile();
      // // try {
      // if (old.exists()) {
      // old.rename(dest);
      // old.meta("", ".meta.json").asPathLocation().rename(dest.meta("",
      // ".meta.json").asPathLocation());
      // old.meta("", ".links.json").asPathLocation().rename(dest.meta("",
      // ".links.json").asPathLocation());
      // }
      boolean forcedDownload = exists && config.forceDownload(href, contentUid.asWritableFile());
      if (!exists || forcedDownload) {
        String token = tokenQueue.take();
        // semaphore.acquire();
        // int available = counter.incrementAndGet();
        try {
          // check writing before downloading
          destInitial.write("").deleteFile(DeleteOptions.deletePermanent());
          log.debug("download from url #{} [{}]", token, href.externalForm);
          try {
            String url = href.externalForm;
            RequestResponse content = download(href, destInitial);
            metadata = content.getMetadata();
            metadata.addField("crawler.slug", href.slug());
            dest = config.cached(Slug.contentPathFinal(href.externalForm, metadata)).asWritableFile();
            contentUid.asPathLocation().deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
            // Adjust userSymlinkTo to point to the canonical file name (without .gz), even
            // if physical file is .gz?
            // Symlink usually points to existing file.
            // If we only store compressed, dest (without .gz) doesn't exist.
            // So we might need to point to dest + ".gz" if that's what we kept.

            WritableFileLocation finalDest = dest;
            if (config.contentStorage == ContentStorage.compressed && !dest.exists()) {
              String ext = getExtensionForEncoding(getEncoding(metadata));
              if (!ext.isEmpty()) {
                WritableFileLocation compDest = dest.parentRef().get().child(dest.filename() + ext).asWritableFile();
                if (compDest.exists()) {
                  finalDest = compDest;
                }
              }
            }

            contentUid.userSymlinkTo(finalDest); // Rename logic needs to handle .gz, .br, .zst
            if (destInitial.exists()) {
              if (!isSame(destInitial, dest)) {
                destInitial.rename(dest.backupIfExists());
              }
            }

            String ext = getExtensionForEncoding(getEncoding(metadata));
            if (!ext.isEmpty()) {
              ReferenceLocation compInitial = destInitial.parentRef().get().child(destInitial.filename() + ext);
              if (compInitial.exists()) {
                ReferenceLocation compDest = dest.parentRef().get().child(dest.filename() + ext);
                if (!isSame(compInitial, compDest)) {
                  compInitial.asWritableFile().rename(compDest.asWritableFile().backupIfExists());
                }
              }
            }

            ReferenceLocation metaJson = dest.meta("", ".meta.json");
            // dest.touch();
            // metaJson.asPathLocation().write("").deleteFile(DeleteOptions.deletePermanent());
            metaJson.asPathLocation().write(content.computeMetadata(metadata));
          } catch (Exception e) {
            String rootMessage = Throwables.getRootCause(e).getMessage();
            Throwable rootCause = Throwables.getRootCause(e);
            // Don't mark server as failing for client-side issues (bad URLs, validation
            // errors)
            // These are link extraction issues, not server issues
            if (rootMessage != null && rootMessage.contains("unknown protocol")) {
              log.debug("skipping unsupported protocol [{}]: {}", href.externalForm, rootMessage);
              destInitial.deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
              return API.Seq();
            }
            // IllegalArgumentException from RequestResponse validation means bad
            // URL/response
            // (e.g., template variables like ${i.uri}, null status code, etc.)
            if (rootCause instanceof IllegalArgumentException) {
              String location = e.getStackTrace().length > 0 ? e.getStackTrace()[0].getClassName() : "";
              boolean isWebClientValidation = location.contains("WebClientLocation")
                  || location.contains("RequestResponse");
              log.info("skipping invalid URL/response [{}]: {} ({}{})",
                  href.externalForm,
                  rootMessage != null ? rootMessage : "validation failed",
                  rootCause.getClass().getSimpleName(),
                  isWebClientValidation ? " from WebClient validation" : "");
              destInitial.deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
              return API.Seq();
            }
            log.info("mark failing server[{}]: {}", hostname, rootMessage);
            failingServers.put(hostname, href.externalForm);
            if (metadata == null)
              metadata = Metadata.error(href.externalForm, e);
            else
              metadata.error = RichThrowable.toString(e);
            contentUid.asPathLocation().deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
            contentUid.userSymlinkTo(destInitial);
            ReferenceLocation metaJson = destInitial.meta("", ".meta.json");
            metaJson.asPathLocation().write(Nodes.json.toString(metadata));
          }
        } finally {
          log.debug("download from url #{} done [{}]", token, href.externalForm);
          tokenQueue.put(token);
        }
      } else {
        dest = destFromSymlink.get().asWritableFile();
        // If symlink points to .gz, we need to strip extension to get "logical" dest?
        // No, dest is just used for metadata sibling resolution.
        if (dest.extension().equals("gz")) {
          dest = dest.parentRef().get().child(org.apache.commons.io.FilenameUtils.removeExtension(dest.filename()))
              .asWritableFile();
        }

        // recompute meta
        if (config.migrate) {
          ReferenceLocation metaJson3 = dest.meta("", ".meta.json");
          String metaContent = metaJson3.asReadableFile().readContent();
          Metadata meta = metaContent.isEmpty() ? Metadata.empty(href.externalForm)
              : Nodes.json.toObject(metaContent, Metadata.class);
          WritableFileLocation destRecomputed = config.cached(Slug.contentPathFinal(href.externalForm, meta))
              .asWritableFile();
          var metaJson3Recomputed = destRecomputed.meta("", ".meta.json").asWritableFile();
          if (!isSame(metaJson3, metaJson3Recomputed)) {
            metaJson3.asWritableFile()
                .rename(metaJson3Recomputed.mkdirOnParentIfNeeded().toPathLocation().backupIfExists());
          }
          if (!isSame(dest, destRecomputed)) {
            // Handle potential compressed rename
            if (dest.exists()) {
              dest.rename(destRecomputed.mkdirOnParentIfNeeded().toPathLocation().backupIfExists());
            }

            String encoding = getEncoding(meta);
            String ext = getExtensionForEncoding(encoding);
            if (!ext.isEmpty()) {
              ReferenceLocation compSrc = dest.parentRef().get().child(dest.filename() + ext);
              if (compSrc.exists()) {
                ReferenceLocation compDst = destRecomputed.parentRef().get().child(destRecomputed.filename() + ext);
                if (!isSame(compSrc, compDst)) {
                  compSrc.asWritableFile().rename(compDst.asWritableFile().backupIfExists());
                }
              }
            }
          }

          WritableFileLocation finalDest = destRecomputed;
          if (config.contentStorage == ContentStorage.compressed && !destRecomputed.exists()) {
            String ext = getExtensionForEncoding(getEncoding(meta));
            if (!ext.isEmpty()) {
              WritableFileLocation compDest = destRecomputed.parentRef().get().child(destRecomputed.filename() + ext)
                  .asWritableFile();
              if (compDest.exists()) {
                finalDest = compDest;
              }
            }
          }
          contentUid.userSymlinkTo(finalDest);
          // links should also be renamed?

          dest = destRecomputed;
          metadata = meta;
        } else {
          dest = config.cached(Slug.contentPathFinal(href.externalForm, metadata)).asWritableFile();
        }
      }

      Traversable<HyperLink> links = null;
      if (metadata != null) {
        if (metadata.error != null) {
          log.info("mark failing link extraction[{}]: {}", hostname, metadata.error);
          links = API.Seq();
        } else
          try {
            links = extractLinksInMemory(href, dest, metadata);
          } catch (Exception e) {
            log.info("mark failing link extraction[{}]: {}", hostname, Throwables.getRootCause(e).getMessage(), e);
            links = API.Seq();
          }
      } else {
        try {
          String type = href.isResource ? "RES" : "PAGE";
          log.info("{}>{} HIT", type, href.externalForm);
          ReferenceLocation metaJson3 = dest.meta("", ".meta.json");
          String metaContent = metaJson3.asReadableFile().readContent();
          Metadata meta = metaContent.isEmpty() ? new Metadata() : Nodes.json.toObject(metaContent, Metadata.class);
          links = extractLinksInMemory(href, dest, meta);
        } finally {
          log.debug("download from cache done [{}]", href.externalForm);
        }
      }
      // Locations.url(link)
      // .copyToFileAndReturnIt(dest,
      // CopyOptions
      // .copyDoNotOverwriteButIgnore()
      // .withCopyMeta(true)
      // .withDefaultReporting())
      return links
          // Filter out self reference when traversing
          .filter(x -> !x.externalForm.equals(href.externalForm))
          .iterator()
          .groupBy(x -> x.externalForm)
          .map(x -> x._2.head())
          .filter(x -> accept(x))
      // return distinct externalForm - first
      // .map(x -> x._2.head())
      ;
      // } catch (Exception e) {
      // Throwable root = Throwables.getRootCause(e);
      // if (root != null) {
      // if (root instanceof SocketException e3 && e3.getMessage().equals("Connection
      // reset")) {
      // throw e;
      // }
      // }
      // //TODO write in meta the error?
      // log.error("Couldn't extract links from {} - {}",
      // dest.absoluteAndNormalized(), href.externalForm, e);
      // return io.vavr.collection.Iterator.empty();
      // }
    }
    //
    // public RequestResponse download(String url) {
    // //return downloadAndExtractLinks(url);
    // throw new RuntimeException("Not implemented yet!!!");
    // }

    public RequestResponse download(HyperLink href, WritableFileLocation dest) {
      String url = href.externalForm;
      HttpClient client2 = HttpClient.newHttpClient();
      Builder reqBuilder = HttpRequest.newBuilder(URI.create(url));
      // GET
      // /corp/xweb/xweb.asp?NTKN=c&page=jobmatches&txtJobId=J1024-0659&clid=21001&jid=1419639
      // HTTP/1.1
      java.util.List<String> headers = headers(
          """
                Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
                Accept-Encoding: gzip, deflate, br, zstd, identity
                Accept-Language: en-US,en;q=0.9,ro;q=0.8,hu;q=0.7
                Referer: https://cgi.njoyn.com/
                Upgrade-Insecure-Requests: 1
                User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36
                Cache-Control: no-cache
                Pragma: no-cache
                Sec-Fetch-Dest: document
                Sec-Fetch-Mode: navigate
                Sec-Fetch-Site: none
                Sec-Fetch-User: ?1
                sec-ch-ua: "Chromium";v="130", "Google Chrome";v="130", "Not?A_Brand";v="99"
                sec-ch-ua-mobile: ?0
                sec-ch-ua-platform: "Windows"
              """,
          "Cookie", "Referer", "Connection", "Host");
      reqBuilder.headers(headers.toArray(new String[0]));
      HttpRequest request = reqBuilder
          .build();
      try {
        HttpResponse<Path> response = client2.send(request,
            HttpResponse.BodyHandlers.ofFile(dest.asPathLocation().toPath()));

        String encoding = response.headers().firstValue("Content-Encoding").orElse("");
        String extension = getExtensionForEncoding(encoding);
        if (!extension.isEmpty()) {
          Path path = dest.asPathLocation().toPath();
          Path tempPath = path.resolveSibling(path.getFileName() + extension);
          // Move original to .<ext>
          java.nio.file.Files.move(path, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

          if (config.contentStorage == ContentStorage.decompressed || config.contentStorage == ContentStorage.both) {
            try (InputStream raw = java.nio.file.Files.newInputStream(tempPath);
                InputStream is = decompressStream(encoding, raw);
                java.io.OutputStream fos = java.nio.file.Files.newOutputStream(path)) {
              is.transferTo(fos);
            }
          }
          if (config.contentStorage == ContentStorage.decompressed) {
            java.nio.file.Files.delete(tempPath);
          }
        }

        // Access request and response details // HttpRequest sentRequest =
        // response.request();
        int statusCode = response.statusCode();
        String type = href.isResource ? "RES" : "PAGE";
        log.info("{}>{} MIS", type, url);
        // return client.get(url).copyTo(dest);
        return new RequestResponse(client.get(url), response);
      } catch (IOException | InterruptedException e) {
        throw org.jedio.RichThrowable.nowrap(e);
      }
    }

    public static java.util.List<String> headers(String headers, String... excludes) {
      java.util.Set<String> excludeSet = java.util.Set.of(excludes);
      java.util.List<String> headerList = new ArrayList<>();

      // Split headers into individual lines
      Iterable<String> lines = Splitter.on("\n").omitEmptyStrings().trimResults().split(headers);
      for (String line : lines) {
        // Split the line by the first occurrence of ":" to separate key and value
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) {
          continue; // Invalid line, skip
        }

        String key = line.substring(0, colonIndex).trim();
        String value = line.substring(colonIndex + 1).trim();

        // Skip excluded headers
        if (excludeSet.contains(key)) {
          continue;
        }

        // Handle splitting by commas only for headers that do not require a single
        // value
        if (key.equalsIgnoreCase("User-Agent") || key.equalsIgnoreCase("Referer") || key.equalsIgnoreCase("Cookie")) {
          headerList.add(key);
          headerList.add(value);
        } else {
          // Use Guava's Splitter to split values on commas and add each as a separate
          // key-value pair
          Iterable<String> values = Splitter.on(",").omitEmptyStrings().trimResults().split(value);
          for (String v : values) {
            headerList.add(key);
            headerList.add(v);
          }
        }
      }

      return headerList;
    }

    // val notParsedUrls = Seq("javascript", "tel")
    // Extract links from content taking into consideration the base url but also
    // the possible <base> tag attribute.
    // <base href="http://www.cartierbratieni.ro/" />
    @SneakyThrows
    private Traversable<HyperLink> extractLinksInMemory(HyperLink parent, WritableFileLocation source,
        Metadata meta) {
      if (meta.responseHeaders == null) {
        return API.Seq();
      }
      JacksonNodes nodes = config.linksNodes;
      ReferenceLocation metaLinks = source.meta("", ".links.json");
      if (metaLinks.exists() && !config.recomputeLinks) {
        try {
          io.vavr.collection.Iterator<@NonNull HyperLink> all = nodes.toIterator(
              metaLinks.asReadableFile().readContent(),
              HyperLink.class);
          HyperLink firstElement = all.headOption().getOrNull();

          // Rebuild the iterator with the first element not consumed
          io.vavr.collection.Iterator<HyperLink> newIterator = firstElement != null
              ? io.vavr.collection.Iterator.concat(io.vavr.collection.Iterator.of(firstElement), all)
              : all;
          return newIterator;
        } catch (com.fasterxml.jackson.databind.RuntimeJsonMappingException e) {
          log.info(
              "Ignoring links cache from {} and read links again for a parsing error. Enable trace for full details:{}",
              metaLinks, e.getMessage());
          log.trace("Ignoring links cache from {} and read links again for a parsing error.", metaLinks, e);
        }
      }
      String content = "";
      if (source.exists()) {
        content = source.asReadableFile().readContent();
      } else {
        String encoding = meta.responseHeaders.getFirst("Content-Encoding");
        String ext = getExtensionForEncoding(encoding);
        if (!ext.isEmpty()) {
          ReferenceLocation compressedSource = source.parentRef().get().child(source.filename() + ext);
          if (compressedSource.exists()) {
            try (InputStream is = decompressStream(encoding, compressedSource.asReadableFile().unsafeInputStream())) {
              content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
          }
        }
      }
      // Option<String> contentType =
      // meta.responseHeaders.getContentType()==MediaType.TEXT_HTML;
      MediaType contentType = meta.responseHeaders.getContentType();
      boolean isHtmlAnd200 = meta.statusCodeValue == 200 && contentType != null
          && (contentType.getType().equals(MediaType.TEXT_HTML.getType())
              ||
              contentType.getSubtype().equals(MediaType.APPLICATION_XML.getSubtype()));
      log.debug("searching links in [{}] from {}", contentType, source);
      String sourceUrl = meta.url;
      // String sourceUrl = meta2.httpMetaRequestUri().get();
      io.vavr.collection.Iterator<HyperLink> result = isHtmlAnd200
          ? extractLinksFromContent(parent.depth + 1, content, source.toExternalForm(), sourceUrl)
          : io.vavr.collection.Iterator.empty();
      List<HyperLink> all = result.toList();
      int status = meta.statusCodeValue;
      if (300 <= status && status < 400 && meta.responseHeaders.getLocation() != null) {
        // String sourceUrl = meta2.httpMetaRequestUri().get();
        String url = meta.responseHeaders.getLocation().toString();
        boolean isResource = false;
        if (url != null) {
          String lower = url.toLowerCase();
          if (lower.endsWith(".pdf") || lower.endsWith(".zip") || lower.endsWith(".mp3") || lower.endsWith(".avi")
              || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".css") || lower.endsWith(".js")) {
            isResource = true;
          }
        }
        all = all
            .append(
                HyperLink.of(url, parent.depth + 1, "Moved - http status " + status, null, "", sourceUrl,
                    source.toExternalForm(), true, isResource));
      }
      // YAMLMapper mapper = Nodes.yml.mapper();
      // mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
      // mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
      // mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
      String c = nodes.toString(all);
      metaLinks.asWritableFile().write(c);
      return all;
    }
  }
}
