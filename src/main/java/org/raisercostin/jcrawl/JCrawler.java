package org.raisercostin.jcrawl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
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
import org.raisercostin.jedio.ReadableFileLocation.Meta;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WebLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.util.Escape;

@Slf4j
public class JCrawler {
  public static void crawl(WebLocation webLocation, DirLocation<?> destination) {
    log.info("crawling [{}] to {}", webLocation, destination);
    List<HttpClientLocation> files = webLocation.ls().map(x -> x.asHttpClientLocation()).toList();
    files.forEach(System.out::println);
    files.forEach(file -> extractLinks(file, file.copyTo(destination.child(slug(file)).asWritableFile(),
      CopyOptions.copyDoNotOverwrite().withDefaultReporting())));
  }

  @Data
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @AllArgsConstructor
  @ToString
  public static class HyperLink {
    HttpClientLocation url;
    WritableFileLocation copied;
    String link;
    @ToString.Exclude
    String text;
    @ToString.Exclude
    String all;

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
  private static Iterator<HyperLink> extractLinks(HttpClientLocation url, WritableFileLocation copied) {
    Meta meta = copied.asReadableFile().readMeta();
    Option<String> contentType = meta.field("payload.response.header.Content-Type");
    boolean isHtml = contentType
      .map(x -> !x.startsWith("image"))
      .getOrElse(false);
    if (isHtml) {
      log.info("searching links in [{}] from {}", contentType.get(), url);
      val content = copied.asReadableFile().readContent();
      val allExp = Iterator.of(r1, r2);
      return allExp.flatMap(exp -> {
        Iterator<Matcher> all = Iterator.continually(exp.matcher(content)).takeWhile(matcher -> matcher.find());
        return all.map(m -> new HyperLink(url, copied, m.group(1).trim(), m.group(2).trim(), m.group().trim()));
      });
    } else {
      log.info("ignoring  links in [{}] from {}", contentType.get(), url);
      return Iterator.empty();
    }
  }

  private static RelativeLocation slug(FileLocation file) {
    return Locations.relative(Escape.toSlug(file.toExternalForm()));
  }
}
