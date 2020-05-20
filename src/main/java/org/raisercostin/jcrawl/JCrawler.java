package org.raisercostin.jcrawl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.MetaInfo;
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
    files.forEach(
      file -> extractLinks(file.copyTo(destination.child(slug(file)).asWritableFile(),
        CopyOptions.copyDoNotOverwrite().withDefaultReporting())));
  }

  @Data
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @AllArgsConstructor
  @ToString
  public static class HyperLink {
    String link;
    @ToString.Exclude
    String text;
    @ToString.Exclude
    String all;
    String sourceUrl;
    WritableFileLocation source;

    @ToString.Include
    String text() {
      return StringUtils.abbreviate(text, 100).replaceAll("\\s+", " ");
    }
  }

  private static Pattern exp(String sep) {
    return Pattern.compile("(?i)(?s)<a[^>]*\\s+href=" + sep + "([^" + sep + "]*)" + sep + "[^>]*>(.*?)</a>");
    //regular expressions with named group consumes 17% of all time
    //("href", "text")
  }

  private final static Pattern r1 = exp("'");
  private final static Pattern r2 = exp("\\\"");

  //val notParsedUrls = Seq("javascript", "tel")
  //Extract links from content taking into consideration the base url but also the possible <base> tag attribute.
  //<base href="http://www.cartierbratieni.ro/" />
  private static Traversable<HyperLink> extractLinks(WritableFileLocation source) {
    MetaInfo meta = source.asReadableFile().readMeta();
    Option<String> contentType = meta.httpResponseHeaderContentType();
    boolean isHtmlAnd200 = contentType
      .map(x -> x.startsWith("text/html") && meta.httpMetaResponseStatusCodeIs200())
      .getOrElse(false);
    log.info("searching links in [{}] from {}", contentType.get(), source);
    Iterator<HyperLink> result = isHtmlAnd200 ? extractLinks(source, meta) : Iterator.empty();
    List<HyperLink> all = result.toList();
    if (meta.httpMetaResponseStatusCode().get().equals("301")) {
      String sourceUrl = meta.httpMetaRequestUri().get();
      all = all
        .append(
          new HyperLink(meta.httpMetaResponseHeaderLocation().get(), "Moved Permanently - 301", "", sourceUrl, source));
    }

    YAMLMapper mapper = Nodes.yml.mapper();
    mapper.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
    mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
    mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
    String c = Nodes.yml.toString(all);
    source.meta("links", "yml").asWritableFile().write(c);
    return all;
  }

  private static Iterator<HyperLink> extractLinks(WritableFileLocation source, MetaInfo meta) {
    Iterator<HyperLink> result;
    String sourceUrl = meta.httpMetaRequestUri().get();
    val content = source.asReadableFile().readContent();
    val allExp = Iterator.of(r1, r2);
    result = allExp.flatMap(exp -> {
      Iterator<Matcher> all = Iterator.continually(exp.matcher(content)).takeWhile(matcher -> matcher.find());
      return all.map(m -> new HyperLink(m.group(1).trim(), m.group(2).trim(), m.group().trim(), sourceUrl, source));
    });
    return result;
  }

  private static RelativeLocation slug(FileLocation file) {
    return Locations.relative(Escape.toSlug(file.toExternalForm()));
  }
}
