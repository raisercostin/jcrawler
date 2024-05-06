package org.raisercostin.jcrawl;

import java.util.regex.Matcher;

import com.google.common.collect.Lists;
import io.vavr.API;
import io.vavr.Tuple3;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import lombok.ToString;
import org.jedio.regex.RichRegex;
import org.jedio.struct.RichIterable;

/**
 * Generates sequences of strings based on the specified pattern.
 * The method interprets embedded commands within the input string,
 * which direct the expansion of sequences and combinations of possible outputs.
 *
 * <p><b>BNF Notation for Input Pattern:</b></p>
 <pre>{@code
   <pattern> ::= <text> | <text><element><pattern> | <element>
   <element> ::= <range> | <alternatives>
   <range> ::= '{' <number> '-' <number> '}'
   <alternatives> ::= '{' <number> ('|' <number>)+ '}'
   <text> ::= <char> | <char><text>
   <char> ::= any printable ASCII character excluding '{{' and '}}'
   <number> ::= <digit>+
   <digit> ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
 }</pre>
 */
public class Generators {
  public static Seq<String> generate(String generator) {
    return parse(generator).generate();
  }

  public static Generator parse(String generator) {
    //String patternString = "(?<before>(?:[^\\\\{]|\\\\[^{])*)(?:\\{(?<internal>(?:[^\\\\}]|\\\\[^}])*)\\})?";
    String patternString = "(?<before>[^{]*)(?:\\{(?<internal>[^}]*)\\}|(?<end>.*$))";
    Either<String, Matcher> all = RichRegex.regexpMatcher(patternString, generator, 3);
    if (all.isRight()) {
      AndGenerator and = new AndGenerator();
      for (;;) {
        Matcher matcher = all.get();
        and.add(new ConstGenerator(matcher.group("before")));
        String value = matcher.group("internal");
        if (value != null) {
          and.add(parseInternal(value));
        }
        String endValue = matcher.group("end");
        if (endValue != null) {
          and.add(new ConstGenerator(endValue));
        }
        if (matcher.find()) {
        } else {
          break;
        }
      }
      return and;
    } else {
      return new ConstGenerator(generator);
    }
  }

  public static Generator parseInternal(String internal) {
    Either<String, Tuple3<Matcher, String, String>> rangePart = RichRegex.regexp2("(\\d+)-(\\d+)", internal);
    if (rangePart.isRight()) {
      //it's a range
      return new RangeGenerator(Integer.parseInt(rangePart.get()._2), Integer.parseInt(rangePart.get()._3));
    }
    OrGenerator orGenerator = new OrGenerator();
    Either<String, Matcher> orParts = RichRegex.regexpMatcher("(?<before>[^|]*)\\|?", internal, 1);
    if (orParts.isRight()) {
      for (;;) {
        Matcher matcher = orParts.get();
        String value = matcher.group("before");
        if (value.isEmpty()) {
          break;
        }
        orGenerator.add(new ConstGenerator(value));
        if (matcher.find()) {
        } else {
          break;
        }
      }
      return orGenerator;
    }
    return new ConstGenerator(internal);
  }

  public interface Generator {
    Seq<String> generate();
  }

  @ToString
  public static class ConstGenerator implements Generator {
    public String text;

    public ConstGenerator(String text) {
      this.text = text;
    }

    @Override
    public Seq<String> generate() {
      return API.Seq(text);
    }
  }

  @ToString
  public static class RangeGenerator implements Generator {
    public int start;
    public int end;

    public RangeGenerator(int start, int end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public Seq<String> generate() {
      return List.range(start, end + 1).map(x -> "" + x);
    }
  }

  @ToString
  public static class OrGenerator implements Generator {
    Seq<Generator> all = API.Seq();

    @Override
    public Seq<String> generate() {
      return all.flatMap(x -> x.generate());
    }

    public void add(ConstGenerator value) {
      all = all.append(value);
    }
  }

  @ToString
  public static class AndGenerator implements Generator {
    Seq<Generator> all = API.Seq();

    @Override
    public Seq<String> generate() {
      return RichIterable.ofJava(Lists.cartesianProduct(all.map(x -> x.generate().toJavaList()).toJavaList()))
        .map(x -> RichIterable.ofJava(x).mkString())
        .toList();
    }

    public void add(Generator value) {
      all = all.append(value);
    }
  }
}