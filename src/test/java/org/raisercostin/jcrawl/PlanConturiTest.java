package org.raisercostin.jcrawl;

import java.util.List;

import io.vavr.collection.Iterator;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.nodes.Nodes;

public class PlanConturiTest {

  public enum ContType {
    A,
    P,
    AP
  }

  public static class Cont {
    public String code;
    public String name;
    public ContType type;
    public String comment;
  }

  @Test
  void testPlanConturi() {
    //TODO read from lege https://legislatie.just.ro/Public/DetaliiDocument/269988
    String planConturi = Locations.readableFile("d:\\home\\raiser\\work\\2024-06-04 - conta spoon\\plan-de-conturi.md")
      .readContent();

    String from = "Planul de conturi general este următorul:";
    String to = "Transpunerea conturilor din balanța de verificare";
    planConturi = StringUtils.substring(planConturi, planConturi.indexOf(from) + from.length(),
      planConturi.indexOf(to));
    //    System.out.println(planConturi);
    String sep = "\t";

    planConturi = planConturi
      //clasa is also a number
      .replaceAll("(?m)^\\s*Clasa (\\d+) - (.*)$", "$1" + sep + "$2")
      //eliminate multiline
      .replaceAll("\n\\s*([^\\d\\s])", " $1")
      .replaceAll("(?m)\\*\\d+\\)", "")
      .replaceAll("(?m)\\(P\\)$", sep + "P")
      .replaceAll("(?m)\\(A\\)$", sep + "A")
      .replaceAll("(?m)\\(A/P\\)$", sep + "AP")

      //comments
      .replaceAll("(?m)Notă(.*)$", sep + "comment1:$1")
      .replaceAll("(?m)----------(.*)$", sep + "comment2:$1")
      .replaceAll("(?m)\\(([^)]+)\\)", sep + "comment3:$1")
    //.replaceAll("(?m)\\([^" + sep + "]+($|\\" + sep + ")", "#comment3");
    //
    ;

    //
    planConturi = planConturi
      .replaceAll("(?m)^\\s*(\\d{1,4})\\.?\\s+(.*)$", "$1" + sep + "$2")
      .replaceAll("(?m)\s+[" + sep + "]", sep);

    List<Cont> conturi = Nodes.csv
      .withCsvSchema(s -> s.withColumnSeparator(sep.charAt(0)))
      .toList(planConturi, Cont.class);
    System.out.println(Iterator.ofAll(conturi).mkString("\n"));

    Locations.path("d:\\home\\raiser\\work\\2024-06-04 - conta spoon\\plan-de-conturi.tsv").write(planConturi);
  }
}
