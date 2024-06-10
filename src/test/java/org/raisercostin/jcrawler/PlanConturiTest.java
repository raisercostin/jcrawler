package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.csv.CsvParser.Feature;
import io.vavr.collection.Iterator;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.nodes.Nodes;

public class PlanConturiTest {

  public enum ContType {
    A,
    P,
    AP
  }

  @AllArgsConstructor
  @ToString
  public static class Cont {
    @JsonProperty(index = 0)
    public String code;
    @JsonProperty(index = 1)
    public String name;
    @JsonProperty(index = 2)
    public ContType type;
    @JsonProperty(index = 3)
    public String comment;
  }

  @ToString
  public static class ContRaw {
    public String code;
    public String name;
    public String type;
    public String comment;

    @SuppressWarnings("null")
    public Cont fix() {
      ContType type2 = Try.of(() -> ContType.valueOf(type)).getOrNull();
      boolean isNull = type2 == null;
      if (!isNull) {
        return new Cont(code, name, type2, comment);
      } else {
        return new Cont(code, name, null, type == null ? comment : comment == null ? type : type + comment);
      }
    }
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
    //Do not use # as is for comments
    String sep = "@";

    planConturi = planConturi
      //clasa is also a number
      .replaceAll("(?m)^\\s*Clasa (\\d+) - (.*)$", "$1" + sep + "$2")
      //eliminate multiline
      .replaceAll("\n\\s*([^\\d\\s])", " $1")
      .replaceAll("(?m)\\*\\d+\\)", "")
      .replaceAll("(?m)\\(P\\)", sep + "P")
      .replaceAll("(?m)\\(A\\)", sep + "A")
      .replaceAll("(?m)\\(A/P\\)", sep + "AP")

      //comments
      .replaceAll("(?m)Notă(.*)$", sep + "comment1:$1")
      .replaceAll("(?m)----------(.*)$", sep + "comment2:$1")
      .replaceAll("(?m)\\(([^)]+)\\)", sep + "comment3:$1")
    //
    //      .replaceAll("(?m)@comment\\d:([^@]+)@([AP]+)(@|$)", "@$2@comment4:$1$3")
    //
    ;

    //
    planConturi = planConturi
      .replaceAll("(?m)^\\s*(\\d{1,4})\\.?\\s+(.*)$", "$1" + sep + "$2")
      .replaceAll("(?m)\s+[" + sep + "]", sep)
      .replaceAll("(?m)\\s*comment\\s*\\d\\s*:\\s*", "");

    planConturi = "code%sname%stype%scomment\n".formatted(sep, sep, sep) + planConturi;
    System.out.println(planConturi);
    List<Cont> conturi = RichIterable.ofAll(Nodes.csv
      .withMapper(m -> m
        //.disable(Feature.FAIL_ON_MISSING_HEADER_COLUMNS)
        .enable(Feature.IGNORE_TRAILING_UNMAPPABLE))
      .withCsvSchema(s -> s.withColumnSeparator(sep.charAt(0)).withStrictHeaders(true))
      .withIgnoreUnknwon()
      .toList(planConturi, ContRaw.class))
      .map(x -> x.fix())
      .toJavaList();

    System.out.println(Iterator.ofAll(conturi).mkString("\n"));

    Locations.path("d:\\home\\raiser\\work\\2024-06-04 - conta spoon\\plan-de-conturi.tsv").write(planConturi);
    Locations.path("d:\\home\\raiser\\work\\2024-06-04 - conta spoon\\plan-de-conturi.csv")
      .write(Nodes.csv.toString(conturi));
    assertThat(conturi.size()).isEqualTo(645);
  }
}
