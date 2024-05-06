package org.raisercostin.jcrawl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import io.vavr.API;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.Metadata;
import org.raisercostin.jedio.url.WebClientLocation2.WebClientFactory;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.nodes.JacksonNodes;
import org.raisercostin.nodes.Nodes;
import org.springframework.http.MediaType;
import reactor.netty.http.HttpProtocol;

@Slf4j
public class JCrawler {
  public enum TraversalType {
    PARALLEL_BREADTH_FIRST {
      @Override
      public <N> Iterable<N> traverse(CrawlConfig config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return new ParallelGraphTraverser<>(config.maxConnections, successor).startTraversal(todo, config.maxDocs);
      }
    },
    BREADTH_FIRST {
      @Override
      public <N> Iterable<N> traverse(CrawlConfig config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        //TODO bug in guava traversal that checks all initial links twice
        return Traverser.forGraph(successor).breadthFirst(todo);
      }
    },
    DEPTH_FIRST_PREORDER {
      @Override
      public <N> Iterable<N> traverse(CrawlConfig config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return Traverser.forGraph(successor).depthFirstPreOrder(todo);
      }
    },
    DEPTH_FIRST_POSTORDER {
      @Override
      public <N> Iterable<N> traverse(CrawlConfig config, Iterable<N> todo, SuccessorsFunction<N> successor) {
        return Traverser.forGraph(successor).depthFirstPostOrder(todo);
      }
    };

    abstract <N> Iterable<N> traverse(CrawlConfig config, Iterable<N> todo, SuccessorsFunction<N> successor);
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @ToString
  @With
  public static class CrawlConfig {
    public static CrawlConfig start(String... urls) {
      return new CrawlConfig().withStart(API.Seq(urls));
    }

    public static CrawlConfig of(WebLocation webLocation, DirLocation cache,
        Option<ReadableFileLocation> whitelistSample) {
      boolean includeQuery = false;
      Option<Set<String>> whitelist = whitelistSample
        .map(x -> extractLinksFromContent(x.readContent(), null, null)
          .map(z -> z.withoutQuery())
          .toSet());
      Seq<String> start = webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toList();
      return new CrawlConfig(TraversalType.BREADTH_FIRST, Nodes.json, start, cache, webLocation, start.toSet(),
        includeQuery,
        whitelist,
        -1, 3, null, Duration.ofDays(100), null);
    }

    /**Breadth first is usual.*/
    public TraversalType traversalType = TraversalType.BREADTH_FIRST;
    public JacksonNodes linksNodes = Nodes.json;
    public Seq<String> start;
    public DirLocation cache;
    public WebLocation webLocation;
    public Set<String> children;
    public boolean includeQuery;
    public Option<Set<String>> exactMatch;
    public int maxDocs = -1;
    public int maxConnections = 3;
    public HttpProtocol[] protocols;
    public TemporalAmount cacheExpiryDuration = Duration.ofDays(100);
    public String generator;

    public boolean accept(HyperLink link) {
      if (exactMatch == null || exactMatch.isEmpty() || exactMatch.get().isEmpty()) {
        return acceptStarts(link.externalForm);
      }
      return exactMatch.get().contains(link.externalForm) || acceptStarts(link.externalForm);
    }

    public boolean acceptStarts(String url) {
      return children.exists(x -> url.startsWith(x));
    }

    public CrawlConfig withFiltersByPrefix(String... filters) {
      return withChildren(API.Set(filters));
    }

    public CrawlConfig withProtocol(HttpProtocol... protocols) {
      return withProtocols(protocols);
    }

    public boolean forceDownload(HyperLink href, WritableFileLocation dest) {
      return dest.modifiedDateTime().toInstant().isBefore(Instant.now().minus(cacheExpiryDuration));
    }
  }

  public static RichIterable<String> crawl(CrawlConfig config) {
    JCrawler crawler = new JCrawler(config);
    if (config.generator != null) {
      return crawler.crawl(Generators.parse(config.generator).generate().map(x -> HyperLink.of(x)));
    }
    return crawler.crawl(config.start.map(x -> HyperLink.of(x)));
  }

  public static void crawl(WebLocation webLocation, Option<ReadableFileLocation> whitelistSample,
      DirLocation cache) {
    whitelistSample = whitelistSample.filter(x -> x.exists());
    CrawlConfig config = CrawlConfig.of(webLocation, cache, whitelistSample);
    log.info("crawling [{}] to {}", webLocation, cache);
    Seq<String> files = config.start;
    files.forEach(System.out::println);
    JCrawler crawler = new JCrawler(config);
    crawler.crawl(files.map(url -> HyperLink.of(url)));
  }

  public final CrawlConfig config;
  public final WebClientFactory client;
  //public final java.util.concurrent.Semaphore semaphore;
  public final BlockingQueue<String> tokenQueue;

  public JCrawler(CrawlConfig config) {
    this.config = config;
    this.client = new WebClientFactory(config.protocols);
    //this.semaphore = new Semaphore(config.maxConnections);
    this.tokenQueue = new ArrayBlockingQueue<>(config.maxConnections);

    //Populate the queue with tokens
    for (int i = 0; i < config.maxConnections; i++) {
      tokenQueue.add("c" + i);
    }
  }

  private RichIterable<String> crawl(Traversable<HyperLink> todo) {
    Iterable<HyperLink> all = config.traversalType.traverse(config, todo, this::downloadAndExtractLinks);
    return RichIterable.ofAll(all).map(x -> x.externalForm).take(config.maxDocs);
  }

  private Traversable<HyperLink> downloadAndExtractLinks(HyperLink href) {
    if (!config.accept(href)) {
      log.info("ignored [{}]", href.externalForm);
      return io.vavr.collection.Iterator.empty();
    }
    try {
      WritableFileLocation dest = config.cache.child(slug(href)).asWritableFile();

      Traversable<HyperLink> links = null;
      boolean exists = dest.exists();
      boolean forcedDownload = exists && config.forceDownload(href, dest);
      ReferenceLocation metaJson = dest.meta("", ".meta.json");
      if (!exists || forcedDownload) {
        String token = tokenQueue.take();
        //semaphore.acquire();
        //int available = counter.incrementAndGet();
        try {
          log.info("download from url #{} [{}]", token, href.externalForm);
          RequestResponse content = client.get(href.externalForm).readCompleteContentSync(null);
          String body = content.getBody();
          dest.write(body);
          Metadata metadata = content.getMetadata();
          metaJson.asPathLocation().write(content.computeMetadata(metadata));
          links = extractLinksInMemory(body, dest, metadata);
        } finally {
          log.info("download from url #{} done [{}]", token, href.externalForm);
          tokenQueue.put(token);
        }
      } else {
        try {
          log.info("download from cache [{}]", href.externalForm);
          links = extractLinksFromDisk(dest, metaJson);
        } finally {
          log.info("download from cache done [{}]", href.externalForm);
        }
      }
      //Locations.url(link)
      //        .copyToFileAndReturnIt(dest,
      //          CopyOptions
      //            .copyDoNotOverwriteButIgnore()
      //            .withCopyMeta(true)
      //            .withDefaultReporting())
      return links
        //Filter out self reference when traversing
        .filter(x -> !x.externalForm.equals(href.externalForm))
        .filter(config::accept)
        //return distinct externalForm - first
        .iterator()
        .groupBy(x -> x.externalForm)
        .map(x -> x._2.head());
    } catch (Exception e) {
      //TODO write in meta the error?
      log.error("couldn't extract links from {}", href.externalForm, e);
      return io.vavr.collection.Iterator.empty();
    }
  }

  private Traversable<HyperLink> extractLinksFromDisk(WritableFileLocation source, ReferenceLocation metaJson) {
    String metaContent = metaJson.asReadableFile().readContent();
    Metadata meta = Nodes.json.toObject(metaContent, Metadata.class);
    String content = source.asReadableFile().readContent();
    return extractLinksInMemory(content, source, meta);
  }

  //val notParsedUrls = Seq("javascript", "tel")
  //Extract links from content taking into consideration the base url but also the possible <base> tag attribute.
  //<base href="http://www.cartierbratieni.ro/" />
  private Traversable<HyperLink> extractLinksInMemory(String content, WritableFileLocation source,
      Metadata meta) {
    JacksonNodes nodes = config.linksNodes;
    ReferenceLocation metaLinks = source.meta("", ".links.json");
    if (metaLinks.exists()) {
      try {
        io.vavr.collection.Iterator<@NonNull HyperLink> all = nodes.toIterator(metaLinks.asReadableFile().readContent(),
          HyperLink.class);
        HyperLink firstElement = all.headOption().getOrNull();

        // Rebuild the iterator with the first element not consumed
        io.vavr.collection.Iterator<HyperLink> newIterator = firstElement != null
            ? io.vavr.collection.Iterator.concat(io.vavr.collection.Iterator.of(firstElement), all)
            : all;
        return newIterator;
      } catch (com.fasterxml.jackson.databind.RuntimeJsonMappingException e) {
        log.info(
          "Ignoring links cache from {} and read links again for a parsing error. Enable trace for full details.",
          metaLinks, e.getMessage());
        log.trace("Ignoring links cache from {} and read links again for a parsing error.", metaLinks, e);
      }
    }
    //Option<String> contentType = meta.responseHeaders.getContentType()==MediaType.TEXT_HTML;
    boolean isHtmlAnd200 = meta.responseHeaders.getContentType().getType().equals(MediaType.TEXT_HTML.getType())
        &&
        meta.statusCodeValue == 200;
    log.info("searching links in [{}] from {}", meta.responseHeaders.getContentType(), source);
    String sourceUrl = meta.url;
    //String sourceUrl = meta2.httpMetaRequestUri().get();
    io.vavr.collection.Iterator<HyperLink> result = isHtmlAnd200
        ? extractLinksFromContent(content, source.toExternalForm(), sourceUrl)
        : io.vavr.collection.Iterator.empty();
    List<HyperLink> all = result.toList();
    int status = meta.statusCodeValue;
    if (300 <= status && status < 400 && meta.responseHeaders.getLocation() != null) {
      //String sourceUrl = meta2.httpMetaRequestUri().get();
      all = all
        .append(
          HyperLink.of(meta.responseHeaders.getLocation().toString(), "Moved Permanently - 301", null, "", sourceUrl,
            source.toExternalForm()));
    }
    //      YAMLMapper mapper = Nodes.yml.mapper();
    //      mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
    //      mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
    //      mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
    String c = nodes.toString(all);
    metaLinks.asWritableFile().write(c);
    return all;
  }

  private static Pattern exp(String sep) {
    return Pattern
      .compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "#]*)(#[^" + sep + "]*)?" + sep + "[^>]*>(.*?)</a>");
    //regular expressions with named group consumes 17% of all time
    //("href", "text")
  }

  private final static Seq<Pattern> allExp = API.Seq(exp("'"), exp("\\\""));

  private static io.vavr.collection.Iterator<HyperLink> extractLinksFromContent(final String content, String source,
      String sourceUrl) {
    io.vavr.collection.Iterator<HyperLink> result;
    result = allExp.iterator().flatMap(exp -> {
      io.vavr.collection.Iterator<Matcher> all = io.vavr.collection.Iterator.continually(exp.matcher(content))
        .takeWhile(matcher -> matcher.find());
      return all.map(
        m -> HyperLink.of(m.group(1).trim(), m.group(3).trim(), m.group(2), m.group().trim(), sourceUrl,
          source));
    });
    return result;
  }

  private static RelativeLocation slug(HyperLink file) {
    return slug(file.externalForm);
  }

  private static RelativeLocation slug(String url) {
    return Locations.relative(SlugEscape.toSlug(url));
  }
}
