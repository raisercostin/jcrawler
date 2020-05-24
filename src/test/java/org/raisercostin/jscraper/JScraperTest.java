package org.raisercostin.jscraper;

import static io.vavr.API.println;

import java.util.LinkedList;
import java.util.function.Supplier;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Vector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jscraper.JScraper.Page;
import org.raisercostin.jscraper.diff_match_patch.Operation;
import org.raisercostin.nodes.Nodes;

@Slf4j
class JScraperTest {
  @Test
  void test() {
    JScraper.scrape(Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/"));
  }

  @Test
  void test2() {
    List<PathLocation> all = Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan4-restocracy-diff/")
      .ls()
      .toList();
    String a = all.get(0).readContent();
    String b = all.get(1).readContent();
    diff_match_patch dmp = new diff_match_patch();
    dmp.Diff_EditCost = 10;
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b, true);
    // Result: [(-1, "Hell"), (1, "G"), (0, "o"), (1, "odbye"), (0, " World.")]
    dmp.diff_cleanupSemantic(diff);
    // Result: [(-1, "Hello"), (1, "Goodbye"), (0, " World.")]
    Locations.writableFile("target/diff2.html").write(dmp.diff_prettyHtml(diff));
    dump(diff);
    Nodes.yml.mapper().configure(Feature.LITERAL_BLOCK_STYLE, true);
    Nodes.yml.mapper().configure(Feature.MINIMIZE_QUOTES, true);
    //System.out.println(Nodes.yml.toString(diff));
  }

  private void dump(LinkedList<diff_match_patch.Diff> diff) {
    Iterator.ofAll(diff)
      //.filter(x -> x.operation.equals(Operation.EQUAL) && x.text.length() > 200)
      .zipWithIndex()
      .map(x -> String.format("*****\n%s-size %s - #%s\n******\n%s", x._1.operation, x._1.text.length(), x._2,
        x._1.text.replace("\\n", "\\n\n")))
      .forEach(x -> println(x));
  }

  @Test
  void test3() {
    PathLocation crawlDir = Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/");
    List<Page> all = crawlDir
      .ls()
      .filter(x -> !x.filename().startsWith("."))
      .map(x -> Page.fromCrawl(x))
      //.take(10)
      .toList();
    String template = all
      .foldLeft(all.head().jsoupCleanDocument(), (t, page) -> detectTemplate(t, page, crawlDir));

    all.forEach(page -> compare(template, page, crawlDir));
    System.out.println(template);
  }

  private void compare(String template, Page page, PathLocation crawlDir) {
    String a = template;
    String b = page.jsoupCleanDocument();
    diff_match_patch dmp = new diff_match_patch();
    dmp.Diff_EditCost = 10;
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b, true);
    dmp.diff_cleanupSemantic(diff);
    //dump(diff);
    String newTemplate = Iterator.ofAll(diff)
      .filter(x -> x.operation.equals(Operation.INSERT))
      .map(x -> x.text.replace("\\n", "\\n\n"))
      .mkString("\n****\n");
    PathLocation templateDetect = crawlDir.child(".special").child(page.crawl.meta("special", "html").filename());
    templateDetect.write(newTemplate);
  }

  private String detectTemplate(String template, Page page, PathLocation crawlDir) {
    String a = template;
    String b = page.jsoupCleanDocument();

    diff_match_patch dmp = new diff_match_patch();
    dmp.Diff_EditCost = 10;
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b, true);
    dmp.diff_cleanupSemantic(diff);
    //dump(diff);
    String newTemplate = Iterator.ofAll(diff)
      .filter(x -> x.operation.equals(Operation.EQUAL))
      .map(x -> x.text.replace("\\n", "\\n\n"))
      .mkString("\n$param\n");
    crawlDir.child(".template").child(page.crawl.meta("template", "html").filename()).write(newTemplate);
    return newTemplate;
  }

  @Test
  void test4DistancesBetweenPages() {
    //JScraper.scrape(Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/"));
    List<PathLocation> all = Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/")
      .ls()
      .filter(x -> !x.filename().startsWith("."))
      .take(10)
      .toList();
    List<String> template = all
      .map(x -> {
        PathLocation meta = x.meta("jsoup", "html");
        log.info("reading {}", meta);
        return meta.readContent();
      });

    List<List<Integer>> distances = template.map(a -> {
      System.out.println();
      return template.map(b -> distance(a, b));
    });
    System.out.println("all\n" + distances.map(x -> x.mkString(" ")).mkString("\n"));
  }

  private int distance(String a, String b) {
    diff_match_patch dmp = new diff_match_patch();
    dmp.Diff_EditCost = 10;
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b, true);
    dmp.diff_cleanupSemantic(diff);
    int res = dmp.diff_levenshtein(diff);
    System.out.print(res + " ");
    return res;
  }

  @Test
  void test5() {
    PathLocation crawlDir = Locations.existingDir("d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/");
    List<Page> all = crawlDir
      .ls()
      .filter(x -> !x.filename().startsWith("."))
      .map(x -> Page.fromCrawl(x))
      //.take(10)
      .toList();
    List<Map<String, Object>> restoFull = all.map(page -> extractData(page, true).computeValues());
    List<Map<String, Object>> resto = all.map(page -> extractData(page, false).computeValues());
    crawlDir.child(".result/restaurante.html").write(htmlGen(resto));
    crawlDir.child(".result/restaurante-full.html").write(htmlGen(restoFull));
    crawlDir.child(".result/restaurante.csv").write(Nodes.csv.toString(resto.map(x -> x.toJavaMap())));
    crawlDir.child(".result/restaurante.json").write(Nodes.json.toString(restoFull));
    crawlDir.child(".result/restaurante.yml").write(Nodes.yml.toString(restoFull));
  }

  private String htmlGen(List<Map<String, Object>> restoFull) {
    Vector<String> header = restoFull.foldLeft(API.<String>Set(), (set, row) -> set.union(row.keySet()))
      .toVector()
      .sorted();

    String table = "" +
        "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/foundation-sites@6.6.3/dist/css/foundation-float.min.css' crossorigin='anonymous'>"
        + "<table>"
        + "<tr>" + header.map(k -> "<th>" + k + "</th>").mkString() + "</tr>"
        + restoFull
          .map(row -> header.map(k -> row.getOrElse(k, "-")).map(x -> "<td>" + x + "</td>").mkString())
          .map(x -> "<tr>" + x + "</tr>")
          .mkString()
        + "</table>";
    return table;
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
  public static class LazyNode {
    @NonFinal
    Map<String, Object> values = API.Map();
    @NonFinal
    Map<String, Supplier<Object>> suppliers = API.Map();

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> supplier) {
      Tuple2<Object, ? extends Map<String, Object>> a = values.computeIfAbsent(key, k -> (Object) supplier.get());
      values = a._2;
      return (T) a._1;
    }

    public void add(String key, Supplier<Object> supplier) {
      suppliers = suppliers.put(key, supplier);
    }

    public Map<String, Object> computeValues() {
      suppliers.forEach((key, supplier) -> cached(key, () -> emptyToNull(supplier.get())));
      return values;
    }

    private Object emptyToNull(Object object) {
      if (object == null || object instanceof String && ((String) object).isEmpty()) {
        return "-";
      }
      return object;
    }
  }

  private LazyNode extractData(Page page, boolean full) {
    System.out.println("analysing ... " + page.crawl);
    LazyNode node = new LazyNode();
    Elements content = page.jsoup().select("#content");
    node.add("A-title", () -> content.select("h1.blog-title").html().toString());
    node.add("B-site",
      () -> content
        .select("a.blue:not(:contains(facebook)):not(:contains(instagram))")
        .attr("href")
        .toString());
    node.add("C-siteFacebook", () -> content.select("a.blue:contains(facebook)").attr("href").toString());
    node.add("D-siteInstagram",
      () -> content.select("a.blue:contains(instagram)").attr("href").toString());
    node.add("E-telefon", () -> content.select("#telefon").html().toString());
    node.add("F-descriere", () -> content.select("#descriere_pagina").text().toString());
    node.add("G-oras", () -> content.select("#oras").text().toString());
    node.add("H-adresa", () -> content.select("#oras +*").html().toString());
    node.add("I-vizualizari",
      () -> StringUtils.removeEnd(content.select("#hits label").html().toString(), " Vizualizari"));
    node.add("J-commentsNo", () -> content.select("#com_no").text().toString());
    node.add("K-pretMediu", () -> content.select("#pret_mediu #info_resto").html().toString());
    node.add("L-evaluare", () -> content.select("#ultima_evaluare").text().toString());
    node.add("M-updated",
      () -> content.select(".header-title  > .blog-post-meta:eq(0)").html().toString());
    if (full) {
      node.add("N-space", () -> "");
      node.add("O-evaluare", () -> content.select("#ultima_evaluare #info_resto").html().toString());
      node.add("P-program", () -> content.select("div#program.blog-post-meta").toString());
      node.add("Q-full-content", () -> content.select("> .page-header").toString());
      node.add("R-evaluari", () -> content.select("#resto_eval_table").toString());
      node.add("S-evaluariMobile", () -> content.select("#resto_eval_table_mobile").toString());
      node.add("T-full-details", () -> content.select(":has(>[data-carousel-extra]) p").toString());
      node.add("U-full-comments", () -> content.select("#comments").toString());
      node.add("V-full-evaluari", () -> content.select("#evaluari").toString());
      node.add("W-full-map-div", () -> content.select("#map").toString());
      node.add("X-full-map-script", () -> content.select("#map +script").toString());
      node.add("ZB-site",
        () -> content.select("a.blue:not(:contains(facebook)):not(:contains(instagram))").toString());
      node.add("ZC-siteFacebook", () -> content.select("a.blue:contains(facebook)").toString());
      node.add("ZD-siteInstagram", () -> content.select("a.blue:contains(instagram)").toString());
      node.add("ZF-descriere", () -> content.select("#descriere_pagina").html().toString());
      node.add("ZG-oras", () -> content.select("#oras").html().toString());
    }
    return node;
  }
}
