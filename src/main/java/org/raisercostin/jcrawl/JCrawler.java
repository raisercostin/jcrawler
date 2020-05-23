package org.raisercostin.jcrawl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WebLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.nodes.Nodes;

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
      return whitelist.get().contains(link.toExternalForm());
    }
  }

  public static void crawl(WebLocation webLocation, Option<ReadableFileLocation> whitelistSample,
      DirLocation<?> destination) {
    CrawlConfig config = CrawlConfig.of(webLocation, whitelistSample);
    log.info("crawling [{}] to {}", webLocation, destination);
    List<HttpClientLocation> files = webLocation.ls().map(x -> x.asHttpClientLocation()).toList();
    files.forEach(System.out::println);
    Set<String> visited = API.Set();
    crawl(config, visited, toLinks(files), destination);
    //    files.forEach(
    //      file -> extractLinks(file.copyTo(destination.child(slug(file)).asWritableFile(),
    //        CopyOptions.copyDoNotOverwrite().withDefaultReporting())));
  }

  //TODO - breadth first
  //depth first
  private static Set<String> crawl(CrawlConfig config, Set<String> visited2, Traversable<HyperLink> todo,
      DirLocation<?> destination) {
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

  private static Traversable<HyperLink> toLinks(List<HttpClientLocation> files) {
    return files.map(url -> HyperLink.of(url.toExternalForm(), "original", "", null, null));
  }

  private static Traversable<HyperLink> downloadAndExtractLinks(CrawlConfig config, HyperLink hyperLink,
      DirLocation<?> destination) {
    SimpleUrl link = hyperLink.link(config.includeQuery);
    if (accept(config, link)) {
      try {
        return extractLinks(Locations.url(link)
          .copyTo(destination.child(slug(link)).asWritableFile(),
            CopyOptions.copyDoNotOverwrite().withDefaultReporting()));
      } catch (Exception e) {
        log.error("couldn't extract links from {}", hyperLink);
        return Iterator.empty();
      }
    } else {
      log.info("following {} is not allowed", hyperLink);
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
  private static Traversable<HyperLink> extractLinks(WritableFileLocation source) {
    ReferenceLocation metaLinks = source.meta("links", "yml");
    if (!metaLinks.exists()) {
      MetaInfo meta = source.asReadableFile().readMeta();
      Option<String> contentType = meta.httpResponseHeaderContentType();
      boolean isHtmlAnd200 = contentType
        .map(x -> x.startsWith("text/html") && meta.httpMetaResponseStatusCodeIs200())
        .getOrElse(false);
      log.info("searching links in [{}] from {}", contentType, source);
      Iterator<HyperLink> result = isHtmlAnd200 ? extractLinks(source, meta) : Iterator.empty();
      List<HyperLink> all = result.toList();
      int status = meta.httpMetaResponseStatusCode().getOrElse(-1);
      if (300 <= status && status < 400 && meta.httpMetaResponseHeaderLocation().isDefined()) {
        String sourceUrl = meta.httpMetaRequestUri().get();
        all = all
          .append(
            HyperLink.of(meta.httpMetaResponseHeaderLocation().get(), "Moved Permanently - 301", "", sourceUrl,
              source.toExternalForm()));
      }

      YAMLMapper mapper = Nodes.yml.mapper();
      mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
      mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
      mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
      String c = Nodes.yml.toString(all);
      metaLinks.asWritableFile().write(c);
      return all;
    } else {
      return Nodes.yml.toIterator(metaLinks.asReadableFile().readContent(), HyperLink.class);
    }
  }

  private static Pattern exp(String sep) {
    return Pattern.compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "]*)" + sep + "[^>]*>(.*?)</a>");
    //regular expressions with named group consumes 17% of all time
    //("href", "text")
  }

  private final static Seq<Pattern> allExp = API.Seq(exp("'"), exp("\\\""));

  private static Iterator<HyperLink> extractLinks(WritableFileLocation source, MetaInfo meta) {
    Iterator<HyperLink> result;
    String sourceUrl = meta.httpMetaRequestUri().get();
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
