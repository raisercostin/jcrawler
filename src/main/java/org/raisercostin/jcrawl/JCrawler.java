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
import lombok.ToString;
import lombok.Value;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WebLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.nodes.Nodes;
import org.raisercostin.util.Escape;

@Slf4j
public class JCrawler {
  public static void crawl(WebLocation webLocation, DirLocation<?> destination) {
    log.info("crawling [{}] to {}", webLocation, destination);
    List<HttpClientLocation> files = webLocation.ls().map(x -> x.asHttpClientLocation()).toList();
    files.forEach(System.out::println);
    Set<String> visited = API.Set();
    crawl(visited, toLinks(files), destination);
    //    files.forEach(
    //      file -> extractLinks(file.copyTo(destination.child(slug(file)).asWritableFile(),
    //        CopyOptions.copyDoNotOverwrite().withDefaultReporting())));
  }

  //TODO - breadth first
  //depth first
  private static Set<String> crawl(Set<String> visited2, Traversable<HyperLink> todo, DirLocation<?> destination) {
    return todo.foldLeft(visited2, (visited, link) -> {
      ReferenceLocation href = link.link();
      if (!visited.contains(href.toExternalForm())) {
        return crawl(visited.add(href.toExternalForm()), downloadAndExtractLinks(link, destination), destination);
      } else {
        log.info("already visited {}", link);
        return visited;
      }
    });
  }

  private static Traversable<HyperLink> toLinks(List<HttpClientLocation> files) {
    return files.map(url -> HyperLink.of(url.toExternalForm(), "original", "", null, null));
  }

  @Value
  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
  @Slf4j
  public static class HyperLink {
    public static HyperLink of(String relativeOrAbsoluteHyperlink, String text, String all, String sourceHyperlink,
        String sourceLocal) {
      HttpClientLocation link = Locations.url(sourceHyperlink, relativeOrAbsoluteHyperlink);
      //TODO link should not contain #fragments since link is used for uniqueness
      return new HyperLink(link.toExternalForm(), relativeOrAbsoluteHyperlink, text, all, sourceHyperlink, sourceLocal);
    }

    String link;
    String relativeOrAbsoluteHyperlink;
    @ToString.Exclude
    String text;
    @ToString.Exclude
    String all;
    String sourceHyperlink;
    String sourceLocal;

    @ToString.Include
    String text() {
      return StringUtils.abbreviate(text, 100).replaceAll("\\s+", " ");
    }

    public HttpClientLocation link() {
      return Locations.url(link);
    }
  }

  private static Traversable<HyperLink> downloadAndExtractLinks(HyperLink hyperLink,
      DirLocation<?> destination) {
    HttpClientLocation link = hyperLink.link();
    return extractLinks(link.copyTo(destination.child(slug(link)).asWritableFile(),
      CopyOptions.copyDoNotOverwrite().withDefaultReporting()));
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
      if (meta.httpMetaResponseStatusCode().get().equals("301")) {
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
    result = allExp.iterator().flatMap(exp -> {
      Iterator<Matcher> all = Iterator.continually(exp.matcher(content)).takeWhile(matcher -> matcher.find());
      return all.map(
        m -> HyperLink.of(m.group(1).trim(), m.group(2).trim(), m.group().trim(), sourceUrl, source.toExternalForm()));
    });
    return result;
  }

  private static RelativeLocation slug(FileLocation file) {
    return slug(file.toExternalForm());
  }

  private static RelativeLocation slug(String url) {
    return Locations.relative(Escape.toSlug(url));
  }
}
