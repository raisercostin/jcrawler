package org.raisercostin.jscraper;

import static io.vavr.API.println;

import java.util.LinkedList;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.path.PathLocation;
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
    List<PathLocation> all = crawlDir
      .ls()
      .filter(x -> !x.filename().startsWith("."))
      //.take(10)
      .toList();
    String template = all
      .map(x -> {
        PathLocation meta = x.meta("jsoup", "html");
        log.info("reading {}", meta);
        return meta.readContent();
      })
      .reduceLeft((a, b) -> detectTemplate(a, b, crawlDir));
    System.out.println(template);
  }

  private String detectTemplate(String a, String b, PathLocation crawlDir) {
    diff_match_patch dmp = new diff_match_patch();
    dmp.Diff_EditCost = 10;
    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(a, b, true);
    dmp.diff_cleanupSemantic(diff);
    //dump(diff);
    String template = Iterator.ofAll(diff)
      .filter(x -> x.operation.equals(Operation.EQUAL))
      .map(x -> x.text.replace("\\n", "\\n\n"))
      .mkString("\n$param\n");
    crawlDir.child(".template").write(template);
    return template;
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
}
