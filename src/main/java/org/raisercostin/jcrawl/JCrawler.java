package org.raisercostin.jcrawl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    BREADTH_FIRST {
      @Override
      Iterable<HyperLink> traverse(Traverser<HyperLink> traverser, Traversable<HyperLink> todo) {
        return traverser.breadthFirst(todo);
      }
    },
    DEPTH_FIRST_PREORDER {
      @Override
      Iterable<HyperLink> traverse(Traverser<HyperLink> traverser, Traversable<HyperLink> todo) {
        return traverser.depthFirstPreOrder(todo);
      }
    },
    DEPTH_FIRST_POSTORDER {
      @Override
      Iterable<HyperLink> traverse(Traverser<HyperLink> traverser, Traversable<HyperLink> todo) {
        return traverser.depthFirstPostOrder(todo);
      }
    };

    abstract Iterable<HyperLink> traverse(Traverser<HyperLink> traverser, Traversable<HyperLink> todo);
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
        -1, -1, null, Duration.ofDays(100));
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
    public int maxConnections = -1;
    public HttpProtocol[] protocols;
    public TemporalAmount cacheExpiryDuration = Duration.ofDays(100);

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

  //TODO bug in guava traversal that checks all initial links twice
  private RichIterable<String> crawl(Traversable<HyperLink> todo) {
    //Traverser<HyperLink> traverser = Traverser.forGraph(this::downloadAndExtractLinks);
    //Traverser2<HyperLink> traverser2 = Traverser2.forGraph(this::downloadAndExtractLinks);
    //Iterable<HyperLink> traverser2 = config.traversalType.traverse(traverser, todo);
    //Iterable<HyperLink> traverser2 = parallelBreadthFirst(todo);
    //todo.toJavaParallelStream()
    //Iterable<HyperLink> all = traverser2.breadthFirst(todo);

    //TODO analyze if the semaphore is still needed with parallel graph traverser
    Iterable<HyperLink> all = new ParallelGraphTraverser<>(config.maxConnections, this::downloadAndExtractLinks)
      .startTraversal(todo.head());
    return RichIterable.ofAll(all).map(x -> x.externalForm).take(config.maxDocs);
  }

  private Iterable<HyperLink> parallelBreadthFirst(Traversable<HyperLink> todo) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  //
  //  private Set<CrawlNode> fetchLinks(CrawlNode link) {
  //    SimpleUrl href = link.url.link(config.includeQuery);
  //    //if (config.maxDocs < 0 || visited.size() < config.maxDocs) {
  //    //    if (!visited.contains(href.toExternalForm())) {
  //    //      if (config.accept(href)) {
  //    Traversable<HyperLink> newTodo = downloadAndExtractLinks(href);
  //    return crawl(visited.add(href.toExternalForm()), newTodo);
  //  }else
  //
  //  {
  //    log.info("following not allowed for [{}]", link.url.link);
  //    return API.Set();//visited;
  //  }
  //  //    } else {
  //  //      log.info("ignored [{}]", link);
  //  //      return visited;
  //  //    }
  //  //    } else {
  //  //      log.info("already visited [{}]", link);
  //  //      return visited;
  //  //    }
  //  //    } else {
  //  //      log.debug("limit reached [{}]", link);
  //  //      return visited;
  //  //    }
  //
  //  // Simulate fetching and parsing the URL to find linked URLs.
  //  // This should be replaced with real HTTP fetching and HTML parsing logic.
  //  //    return linksDiscovered.computeIfAbsent(url, this::simulateHttpFetch);
  //  }

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
        try {
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
        } finally {
          //counter.decrementAndGet();
          //semaphore.release();
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
