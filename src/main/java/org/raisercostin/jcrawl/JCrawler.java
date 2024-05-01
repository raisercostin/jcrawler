package org.raisercostin.jcrawl;

import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.graph.Traverser;
import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.Metadata;
import org.raisercostin.jedio.url.WebClientLocation2.WebClientFactory;
import org.raisercostin.jedio.url.WebLocation;
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
          .map(z -> SimpleUrl.from(z.link).withoutQuery().toExternalForm())
          .toSet());
      Seq<String> start = webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toList();
      return new CrawlConfig(TraversalType.BREADTH_FIRST, start, cache, webLocation, start.toSet(), includeQuery,
        whitelist,
        -1, -1, null);
    }

    /**Breadth first is usual.*/
    public TraversalType traversalType = TraversalType.BREADTH_FIRST;
    public Seq<String> start;
    public DirLocation cache;
    public WebLocation webLocation;
    public Set<String> children;
    public boolean includeQuery;
    public Option<Set<String>> exactMatch;
    public int maxDocs = -1;
    public int maxConnections = -1;
    public HttpProtocol[] protocols;

    public boolean accept(SimpleUrl link) {
      if (exactMatch == null || exactMatch.isEmpty() || exactMatch.get().isEmpty()) {
        return acceptStarts(link.toExternalForm());
      }
      String url = link.toExternalForm();
      return exactMatch.get().contains(url) || acceptStarts(url);
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
  public final Semaphore semaphore = new Semaphore(5);

  public JCrawler(CrawlConfig config) {
    this.config = config;
    this.client = new WebClientFactory(config.protocols);
  }

  //TODO bug in guava traversal that checks all initial links twice
  private RichIterable<String> crawl(Traversable<HyperLink> todo) {
    Iterable<HyperLink> traverser = config.traversalType.traverse(
      Traverser.forGraph(this::downloadAndExtractLinks), todo);
    return RichIterable.ofAll(traverser).map(x -> x.link).take(config.maxDocs);
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
    SimpleUrl link = href.link(config.includeQuery);
    if (!config.accept(link)) {
      log.info("ignored [{}]", href.link);
      return Iterator.empty();
    }
    try {
      semaphore.acquire();
      log.info("start [{}]", href.link);
      RequestResponse content = client.get(link.toExternalForm()).readCompleteContentSync(null);
      WritableFileLocation dest = config.cache.child(slug(link)).asWritableFile();
      dest.write(content.getBody());
      ReferenceLocation metaJson = dest.meta("", ".meta.json");
      metaJson.asPathLocation().write(content.computeMetadata());
      //Locations.url(link)
      //        .copyToFileAndReturnIt(dest,
      //          CopyOptions
      //            .copyDoNotOverwriteButIgnore()
      //            .withCopyMeta(true)
      //            .withDefaultReporting())
      return extractLinks(dest, metaJson)
        //Filter out self reference when traversing
        .filter(x -> !x.link.equals(href.link));
    } catch (Exception e) {
      //TODO write in meta the error?
      log.error("couldn't extract links from {}", link, e);
      return Iterator.empty();
    } finally {
      log.info("end [{}]", href.link);
      semaphore.release();
    }
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
            HyperLink.of(meta.responseHeaders.getLocation().toString(), "Moved Permanently - 301", null, "", sourceUrl,
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
    return Pattern
      .compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "#]*)(#[^" + sep + "]*)?" + sep + "[^>]*>(.*?)</a>");
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
        m -> HyperLink.of(m.group(1).trim(), m.group(3).trim(), m.group(2), m.group().trim(), sourceUrl,
          source));
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
