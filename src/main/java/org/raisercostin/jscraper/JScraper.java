package org.raisercostin.jscraper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.function.Supplier;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import namek.ExtractorUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.nodes.Nodes;

@Slf4j
public class JScraper {
  public static void scrape(PathLocation crawledDir) {
    crawledDir
      .ls()
      .filter(x -> !x.filename().startsWith("."))
      .map(x -> Page.fromCrawl(x))
      .forEach(x -> x.scrapPage());
    //    crawledDir
    //      .ls()
    //      .filter(x -> !x.filename().startsWith("."))
    //      //.take(1)
    //      .map(x -> scrapePage(x))
    //      .sliding(2)
    //      .forEach(x -> compare(x.get(0), x.get(1)));
  }

  private static Object compare(Page page1, Page page2) {
    //page1.jsoup.children();
    diff_match_patch dmp = new diff_match_patch();
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(page1.jsoupCleanDocument(), page2.jsoupCleanDocument(),
      false);
    // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
    dmp.diff_cleanupSemanticLossless(diff);
    // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
    Nodes.yml.mapper().configure(Feature.LITERAL_BLOCK_STYLE, true);
    Nodes.yml.mapper().configure(Feature.MINIMIZE_QUOTES, true);
    System.out.println(Nodes.yml.toString(diff));
    return null;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
  @Slf4j
  public static class Page {
    public static Page fromCrawl(PathLocation crawl) {
      return new Page(crawl, API.Map());
    }

    PathLocation crawl;
    @NonFinal
    Map<String, Object> attrs = API.Map();
    //    PathLocation metaJsoup;
    //    Document jsoup;
    //    String cleanup;

    public MetaInfo httpMeta() {
      return cached("httpMeta", () -> crawl.readMeta());
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> supplier) {
      Tuple2<Object, ? extends Map<String, Object>> a = attrs.computeIfAbsent(key, k -> (Object) supplier.get());
      attrs = a._2;
      return (T) a._1;
    }

    public PathLocation jsoupLocation() {
      return crawl.meta("jsoup", "html");
    }

    @SneakyThrows
    public Document normalizePage() {
      String url = httpMeta().httpMetaRequestUri().get();
      String content = crawl.readContent();
      content = ExtractorUtils.replaceEntities(content);
      return Jsoup.parse(content, url);
    }

    public String jsoupCleanDocument() {
      Document content = normalizePage();
      content.outputSettings().charset(StandardCharsets.UTF_8);
      content.outputSettings().prettyPrint(true);
      content.outputSettings().indentAmount(2);
      content.outputSettings().escapeMode(EscapeMode.xhtml);
      String cleanup = content.outerHtml();
      return cleanup;
    }

    public void writeJsoupCleanup() {
      PathLocation metaJsoup = jsoupLocation();
      log.info("jsoup in {} from {}", metaJsoup, this);
      metaJsoup.write(jsoupCleanDocument());
    }

    @SneakyThrows
    public void scrapPage() {
      MetaInfo meta = httpMeta();
      if (meta.httpResponseHeaderContentTypeIsHtml()) {
        writeJsoupCleanup();
      }
    }
  }
  //
  //  private static String extractPage(PathLocation page) throws Exception {
  //    return Nodes.json.toString(scala.concurrent.Await.result(new NamekConsole(null)
  //      .extractPage(Locations.file(page.absoluteAndNormalized())),
  //      scala.concurrent.duration.Duration.apply(1000, TimeUnit.SECONDS)));
  //  }
  //    def replaceEntities(content: String) = content.replaceAll("&nbsp;", " ").replaceAll("\n\n", "\n")

  //    ExtractorUtils$.replaceEntities(page.originalContent);
  //    val body2 = Jsoup.parse(content, url)
  //
  //    jsoup = JsoupExtractor.cleanWithJsoup(originUrl, originalContent)(dumpContent);
  //        lazy val content = jsoup.toString
}
