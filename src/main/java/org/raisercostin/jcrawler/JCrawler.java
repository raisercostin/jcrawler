package org.raisercostin.jcrawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
import org.jedio.struct.RichIterable;
import org.raisercostin.jcrawler.RichPicocli.LocationConverter;
import org.raisercostin.jcrawler.RichPicocli.PicocliDir;
import org.raisercostin.jcrawler.RichPicocli.VavrConverter;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.SlugEscape;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.SlugEscape.Slug;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.Metadata;
import org.raisercostin.jedio.url.WebClientLocation2.WebClientFactory;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.nodes.JacksonNodes;
import org.raisercostin.nodes.Nodes;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import reactor.netty.http.HttpProtocol;

@AllArgsConstructor
@ToString
@With
@Command(name = "jcrawl", mixinStandardHelpOptions = true, version = "jcrawl 0.1",
    description = "Crawler tool.",
    //subcommands = GenerateCompletion.class,
    usageHelpAutoWidth = true, usageHelpWidth = 120, showDefaultValues = true,
    versionProvider = JCrawler.MyVersionProvider.class)
@Slf4j
public class JCrawler implements Callable<Integer> {
  public static void main(String[] args) {
    //mainOne("--version", true);
    //mainOne("--debug", true);
    //mainOne("https://raisercostin.org --traversal=BREADTH_FIRST", true);
    main(args, true);
  }

  public static void mainOne(String args, boolean exitAtEnd) {
    main(split(args), exitAtEnd);
  }

  private static String[] split(String cmdWithSpaces) {
    StringTokenizer tokenizer = new StringTokenizer(cmdWithSpaces, ' ', '"');
    tokenizer.setIgnoreEmptyTokens(true);
    return tokenizer.getTokenArray();
  }

  //  git.branch=master
  //      git.build.host=DESKTOP-HO5N784
  //      git.build.time=2024-06-12T20\:53\:44+0300
  //      git.build.user.email=raisercostin@gmail.com
  //      git.build.user.name=raisercostin
  //      git.build.version=0.3-SNAPSHOT
  //      git.closest.tag.commit.count=6
  //      git.closest.tag.name=jcrawler-0.2
  //      git.commit.id=3454d8ad006219cdd9a49f930a31975fab87b334
  //      git.commit.id.abbrev=3454d8a
  //      git.commit.id.describe=jcrawler-0.2-6-g3454d8a
  //      git.commit.id.describe-short=jcrawler-0.2-6
  //      git.commit.message.full=upgraded
  //      git.commit.message.short=upgraded
  //      git.commit.time=2024-06-12T20\:49\:24+0300
  //      git.commit.user.email=raisercostin@gmail.com
  //      git.commit.user.name=raisercostin
  //      git.dirty=false
  //      git.local.branch.ahead=0
  //      git.local.branch.behind=0
  //      git.remote.origin.url=https\://github.com/raisercostin/jcrawler.git
  //      git.tags=
  //      git.total.commit.count=94
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
    //    @JsonAnyGetter
    //    private Map<String, Object> otherProperties = new HashMap<>();
    //
    //    @JsonAnySetter
    //    public void setOtherProperty(String name, Object value) {
    //      otherProperties.put(name, value);
    //    }
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
      //      GitInfo git = Nodes.hocon.toObject(
      //        Locations.classpath("git.properties").readContent(),
      //        GitInfo.class);
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

  private static void main(String[] args, boolean exitAtEnd) {
    IExecutionExceptionHandler errorHandler = (ex, cmd, parseResult) -> {
      JCrawler config = cmd.getCommand();
      log.error("Cmd error.", ex);
      //      if (config.debug) {
      //        ex.printStackTrace(cmd.getErr()); // Print stack trace to the error stream
      //      } else {
      //        cmd.getErr()
      //          .println(
      //            cmd.getColorScheme()
      //              .errorText(ex.getMessage() + ". Use --debug to see stacktrace."));
      //      }
      cmd.getErr().println(cmd.getColorScheme().errorText("Use --help or -h for usage."));
      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    };

    CommandLine cmd = new CommandLine(new JCrawler()).setExecutionExceptionHandler(errorHandler);
    //CommandLine gen = cmd.getSubcommands().get("generate-completion");
    //gen.getCommandSpec().usageMessage().hidden(false);
    int exitCode = cmd.execute(args);
    if (exitAtEnd) {
      System.exit(exitCode);
    }
  }

  public static JCrawler crawler() {
    return new JCrawler();
  }

  public static JCrawler of(WebLocation webLocation, DirLocation cache,
      Option<ReadableFileLocation> whitelistSample) {
    Set<String> whitelist2 = whitelistSample
      .map(x -> extractLinksFromContent(x.readContent(), null, null)
        .map(z -> z.withoutQuery())
        .toSet())
      .getOrNull();
    Seq<String> urls = webLocation.ls().map(x -> x.asHttpClientLocation().toExternalForm()).toList();
    return of(cache, urls).withAdditionalAccepts(whitelist2);
  }

  private static JCrawler of(DirLocation projectDir, Seq<String> urls) {
    JCrawler crawler = new JCrawler(null, TraversalType.BREADTH_FIRST, Nodes.json, new PicocliDir(projectDir), -1, 3,
      null, Duration.ofDays(100), null, null, Verbosity.INFO, false, null, null).withUrlsAndAccept(urls);
    return crawler;
  }

  private static Pattern exp(String sep) {
    return Pattern
      .compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "#]*)(#[^" + sep + "]*)?" + sep + "[^>]*>(.*?)</a>");
    //regular expressions with named group consumes 17% of all time
    //("href", "text")
  }

  private final static Seq<Pattern> allExp = API.Seq(exp("'"), exp("\\\""));

  private static io.vavr.collection.Iterator<HyperLink> extractLinksFromContent(final String content,
      String source,
      String sourceUrl) {
    io.vavr.collection.Iterator<HyperLink> result;
    result = allExp.iterator().flatMap(exp -> {
      io.vavr.collection.Iterator<Matcher> all = io.vavr.collection.Iterator.continually(exp.matcher(content))
        .takeWhile(matcher -> matcher.find());
      return all.map(
        m -> HyperLink.of(m.group(1).trim(), m.group(3).trim(), m.group(2), m.group().trim(), sourceUrl, source));
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
        //TODO bug in guava traversal that checks all initial links twice
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

  public enum Verbosity {
    NONE(Level.OFF),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE);

    final Level level;

    Verbosity(Level logbackLevel) {
      this.level = logbackLevel;
    }

    public void configureLoggingLevel() {
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
      context.getLogger("org.raisercostin.jcrawler").setLevel(level);
    }
  }

  @Spec
  @JsonIgnore
  private CommandSpec spec; // injected by picocli
  @picocli.CommandLine.Option(names = { "-t", "--traversal" },
      description = "Set the traversal mode: ${COMPLETION-CANDIDATES}.")
  public TraversalType traversalType = TraversalType.PARALLEL_BREADTH_FIRST;
  @JsonIgnore
  public JacksonNodes linksNodes = Nodes.json;
  @picocli.CommandLine.Option(names = { "-p", "--project" },
      description = "Project dir for config and crawled content.",
      converter = LocationConverter.class)
  public PicocliDir projectDir = new PicocliDir(Locations.current().child(".jcrawler"));
  @picocli.CommandLine.Option(names = { "-d", "--maxDocs" })
  public int maxDocs = 10_000;
  @picocli.CommandLine.Option(names = { "-c", "--maxConnections" })
  public int maxConnections = 3;
  @picocli.CommandLine.Option(names = { "--protocol" },
      description = "Set the protocol: ${COMPLETION-CANDIDATES}.")
  public HttpProtocol[] protocols = { HttpProtocol.H2, HttpProtocol.HTTP11 };
  @picocli.CommandLine.Option(names = { "--expire" },
      description = "Expiration as a iso 8601 format like P1DT1S. \n Full format P(n)Y(n)M(n)DT(n)H(n)M(n)S\nSee more at https://www.digi.com/resources/documentation/digidocs/90001488-13/reference/r_iso_8601_duration_format.htm")
  public Duration cacheExpiryDuration = Duration.ofDays(100);
  @picocli.CommandLine.Parameters(paramLabel = "urls",
      description = """
          Urls to crawl. If urls contain expressions all combinations of that values will be generated:
          - ranges like {start-end}
          - alternatives like {option1|option2|option3}

          For example https://namekis.com/{docA|doc2}/{1-3} will generate the following urls:
          - https://namekis.com/docA/1
          - https://namekis.com/docA/2
          - https://namekis.com/docA/3
          - https://namekis.com/doc2/1
          - https://namekis.com/doc2/2
          - https://namekis.com/doc2/3""", converter = VavrConverter.class)
  @With(value = AccessLevel.PRIVATE)
  public Seq<String> urls;
  @picocli.CommandLine.Option(names = { "--accept" }, description = "Additional urls to accept.")
  public Set<String> accept;
  @picocli.CommandLine.Option(names = { "-v", "--verbosity" },
      description = "Set the verbosity level: ${COMPLETION-CANDIDATES}.")
  public Verbosity verbosity = Verbosity.WARN;
  @picocli.CommandLine.Option(names = { "--debug" }, description = "Show stack trace")
  public boolean debug = false;
  @picocli.CommandLine.Option(names = { "--acceptHostname" }, description = "Template to accept urls with this prefix.")
  public String acceptHostname = "{http|https}://{www.|}%s";
  public String crawlFormat = "";

  private JCrawler() {
  }

  @Override
  public Integer call() throws Exception {
    //CommandLine.Help help = new CommandLine.Help(spec);
    //System.out.println(help.optionList());
    crawlIterator().forEach(x -> System.out.println(x.externalForm + " => " + x.localCache));
    return 0;
  }

  public RichIterable<HyperLink> crawlIterator() {
    log.info("JCrawler started with config:\n{}", Nodes.yml.toString(this));
    CrawlerWorker worker = worker();
    projectDir.dir.child(".crawl-config.yaml")
      .asWritableFile()
      .nonExistingOrElse(x -> x.delete(DeleteOptions.deleteDefault()))
      //.deleteFile(((SimpleDeleteOptions) DeleteOptions.deleteDefault()).withIgnoreNonExisting(true))
      .asWritableFile()
      .write(Nodes.yml.toString(worker));
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
    RichIterable<Slug> slugs = SlugEscape.slugs(href.externalForm);
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
    Slug slug = SlugEscape.slugs(url).head();
    return (FileLocation) cached(slug);
  }

  public FileLocation slug(HyperLink href) {
    return cachedFile(href.externalForm);
  }

  public JCrawler withFiltersByPrefix(String... filters) {
    return withAccept(API.Set(filters));
  }

  public JCrawler withProtocol(HttpProtocol... protocols) {
    return withProtocols(protocols);
  }

  public boolean forceDownload(HyperLink href, WritableFileLocation dest) {
    return dest.modifiedDateTime().toInstant().isBefore(Instant.now().minus(cacheExpiryDuration));
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

  private JCrawler withAdditionalAccepts(Set<String> additionalAccept) {
    return withAccept(this.accept.addAll(additionalAccept));
  }

  public static class CrawlerWorker {
    public final JCrawler config;
    @JsonIgnore
    public final WebClientFactory client;
    //public final java.util.concurrent.Semaphore semaphore;
    @JsonIgnore
    public final BlockingQueue<String> tokenQueue;
    @JsonIgnore
    public final Cache<String, String> failingServers = CacheBuilder.newBuilder()
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build();
    private Set<String> accept;

    public CrawlerWorker(JCrawler config) {
      config.verbosity.configureLoggingLevel();
      this.config = config;
      this.accept = config.urls != null ? config.urls.iterator()
        .map(
          x -> config.acceptHostname.formatted(HyperLink.of(x).hostnameForAccept()))
        .toSet()
        .addAll(config.accept != null ? config.accept : Collections.emptySet())
        .flatMap(x -> Generators.generate(x)) : API.Set();
      this.client = new WebClientFactory("jcrawler", config.protocols);
      //this.semaphore = new Semaphore(config.maxConnections);
      this.tokenQueue = new ArrayBlockingQueue<>(config.maxConnections);

      //Populate the queue with tokens
      for (int i = 0; i < config.maxConnections; i++) {
        tokenQueue.add("c" + i);
      }
      log.info("Accepts:\n - {}", accept.mkString("\n - "));
    }

    private RichIterable<HyperLink> crawl(Traversable<HyperLink> todo) {
      Iterable<HyperLink> all = config.traversalType.traverse(config, todo, this::downloadAndExtractLinks);
      return RichIterable.ofAll(all)
        .take(config.maxDocs);
    }

    private boolean accept(HyperLink link) {
      boolean accept = accept2(link);
      log.debug("{} [{}]", accept ? "accept" : "ignore", link.externalForm);
      return accept;
    }

    private boolean accept2(HyperLink link) {
      if (accept == null) {
        return false;
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
      if (failingServers.getIfPresent(hostname) != null) {
        log.debug("ignored failing server for a while [{}]", href.externalForm);
        return API.Seq();
      }
      Metadata metadata = null;
      var contentUid = config.cached(SlugEscape.contentUid(href.externalForm));
      var destInitial = config.cached(SlugEscape.contentPathInitial(href.externalForm)).asWritableFile();
      WritableFileLocation dest = destInitial;

      var destFromSymlink = contentUid.asSymlink().map(x -> x.getTarget());
      var metaJson2 = destFromSymlink.map(x -> x.meta("", ".meta.json"));
      boolean exists = contentUid.exists() && contentUid.isSymlink() && destFromSymlink.get().exists()
          && metaJson2.get().exists();
      //      WritableFileLocation old = config.findOldFile(href);
      //      WritableFileLocation dest = config.cached(href).asWritableFile();
      //      //      try {
      //      if (old.exists()) {
      //        old.rename(dest);
      //        old.meta("", ".meta.json").asPathLocation().rename(dest.meta("", ".meta.json").asPathLocation());
      //        old.meta("", ".links.json").asPathLocation().rename(dest.meta("", ".links.json").asPathLocation());
      //      }
      boolean forcedDownload = exists && config.forceDownload(href, contentUid.asWritableFile());
      if (!exists || forcedDownload) {
        String token = tokenQueue.take();
        //semaphore.acquire();
        //int available = counter.incrementAndGet();
        try {
          //check writing before downloading
          destInitial.write("").deleteFile(DeleteOptions.deletePermanent());
          log.debug("download from url #{} [{}]", token, href.externalForm);
          try {
            String url = href.externalForm;
            RequestResponse content = download(url, destInitial);
            metadata = content.getMetadata();
            metadata.addField("crawler.slug", href.slug());
            dest = config.cached(SlugEscape.contentPathFinal(href.externalForm, metadata)).asWritableFile();
            contentUid.asPathLocation().deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
            contentUid.symlinkTo(dest);
            ReferenceLocation metaJson = dest.meta("", ".meta.json");
            //dest.touch();
            //metaJson.asPathLocation().write("").deleteFile(DeleteOptions.deletePermanent());
            metaJson.asPathLocation().write(content.computeMetadata(metadata));
          } catch (Exception e) {
            log.info("mark failing server[{}]: {}", hostname, Throwables.getRootCause(e).getMessage());
            failingServers.put(hostname, href.externalForm);
            metadata = Metadata.error(href.externalForm, e);
            contentUid.asPathLocation().deleteFile(DeleteOptions.deleteByRenameOption().withIgnoreNonExisting(true));
            contentUid.symlinkTo(destInitial);
            ReferenceLocation metaJson = destInitial.meta("", ".meta.json");
            metaJson.asPathLocation().write(Nodes.json.toString(metadata));
          }
        } finally {
          log.info("download from url #{} done [{}]", token, href.externalForm);
          tokenQueue.put(token);
        }
      }

      Traversable<HyperLink> links = null;
      if (metadata != null) {
        if (metadata.error != null)
          links = API.Seq();
        else
          try {
            links = extractLinksInMemory(dest, metadata);
          } catch (Exception e) {
            log.info("mark failing link extraction[{}]: {}", hostname, Throwables.getRootCause(e).getMessage(), e);
            links = API.Seq();
          }
      } else {
        try {
          log.debug("download from cache [{}]", href.externalForm);
          ReferenceLocation metaJson3 = dest.meta("", ".meta.json");
          links = extractLinksFromDisk(dest, metaJson3);
        } finally {
          log.debug("download from cache done [{}]", href.externalForm);
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
        .iterator()
        .groupBy(x -> x.externalForm)
        .map(x -> x._2.head())
        .filter(x -> accept(x))
      //return distinct externalForm - first
      //.map(x -> x._2.head())
      ;
      //      } catch (Exception e) {
      //        Throwable root = Throwables.getRootCause(e);
      //        if (root != null) {
      //          if (root instanceof SocketException e3 && e3.getMessage().equals("Connection reset")) {
      //            throw e;
      //          }
      //        }
      //        //TODO write in meta the error?
      //        log.error("Couldn't extract links from {} - {}", dest.absoluteAndNormalized(), href.externalForm, e);
      //        return io.vavr.collection.Iterator.empty();
      //      }
    }

    public RequestResponse download(String url) {
      //return downloadAndExtractLinks(url);
      throw new RuntimeException("Not implemented yet!!!");
    }

    public RequestResponse download(String url, WritableFileLocation dest) {
      HttpClient client2 = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .build();
      try {
        HttpResponse<Path> response = client2.send(request,
          HttpResponse.BodyHandlers.ofFile(dest.asPathLocation().toPath()));
        // Access request and response details
        HttpRequest sentRequest = response.request();
        int statusCode = response.statusCode();
        log.debug("downloading {} to {}", url, dest.toExternalForm());
        //return client.get(url).copyTo(dest);
        return new RequestResponse(client.get(url), response);
      } catch (IOException | InterruptedException e) {
        throw org.jedio.RichThrowable.nowrap(e);
      }
    }

    private Traversable<HyperLink> extractLinksFromDisk(WritableFileLocation source, ReferenceLocation metaJson) {
      String metaContent = metaJson.asReadableFile().readContent();
      Metadata meta = metaContent.isEmpty() ? new Metadata() : Nodes.json.toObject(metaContent, Metadata.class);
      return extractLinksInMemory(source, meta);
    }

    //val notParsedUrls = Seq("javascript", "tel")
    //Extract links from content taking into consideration the base url but also the possible <base> tag attribute.
    //<base href="http://www.cartierbratieni.ro/" />
    private Traversable<HyperLink> extractLinksInMemory(WritableFileLocation source,
        Metadata meta) {
      if (meta.responseHeaders == null) {
        return API.Seq();
      }
      JacksonNodes nodes = config.linksNodes;
      ReferenceLocation metaLinks = source.meta("", ".links.json");
      if (metaLinks.exists()) {
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
      String content = source.asReadableFile().readContent();
      //Option<String> contentType = meta.responseHeaders.getContentType()==MediaType.TEXT_HTML;
      MediaType contentType = meta.responseHeaders.getContentType();
      boolean isHtmlAnd200 = meta.statusCodeValue == 200 && contentType != null
          && contentType.getType().equals(MediaType.TEXT_HTML.getType());
      log.info("searching links in [{}] from {}", contentType, source);
      String sourceUrl = meta.url;
      //String sourceUrl = meta2.httpMetaRequestUri().get();
      io.vavr.collection.Iterator<HyperLink> result = isHtmlAnd200
          ? extractLinksFromContent(content, source.toExternalForm(), sourceUrl)
          : io.vavr.collection.Iterator.empty();
      List<HyperLink> all = result.toList();
      int status = meta.statusCodeValue;
      if (300 <= status && status < 400 && meta.responseHeaders.getLocation() != null) {
        //String sourceUrl = meta2.httpMetaRequestUri().get();
        String url = meta.responseHeaders.getLocation().toString();
        all = all
          .append(
            HyperLink.of(url, "Moved - http status " + status, null, "", sourceUrl, source.toExternalForm()));
      }
      //      YAMLMapper mapper = Nodes.yml.mapper();
      //      mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
      //      mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
      //      mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
      String c = nodes.toString(all);
      metaLinks.asWritableFile().write(c);
      return all;
    }
  }
}