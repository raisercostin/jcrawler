package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.Generators;

class GeneratorsTest {
  @Test
  void testSimple() {
    assertThat(Generators.generate("a").mkString(",")).isEqualTo("a");
    assertThat(Generators.generate("a{baubau}").mkString(",")).isEqualTo("abaubau");
    assertThat(Generators.generate("a{baubau").mkString(",")).isEqualTo("a{baubau");
    assertThat(Generators.generate("abaubau}").mkString(",")).isEqualTo("abaubau}");
    //assertThat(Generators.generate("a\\{baubau}").mkString(",")).isEqualTo("a{baubau}");
  }

  @Test
  void testSimpleRange() {
    assertThat(Generators.generate("b{1-3}").mkString(",")).isEqualTo("b1,b2,b3");
    assertThat(Generators.generate("b{1-3}c").mkString(",")).isEqualTo("b1c,b2c,b3c");
    assertThat(Generators.generate("{1-3}c").mkString(",")).isEqualTo("1c,2c,3c");

    assertThat(Generators.generate("b{1-3}c{d").mkString(",")).isEqualTo("b1c{d,b2c{d,b3c{d");
  }

  @Test
  void testSimpleAlternatives() {
    assertThat(Generators.generate("b{1|3|foo}").mkString(",")).isEqualTo("b1,b3,bfoo");
    assertThat(Generators.generate("b{1|3|foo}c").mkString(",")).isEqualTo("b1c,b3c,bfooc");
    assertThat(Generators.generate("{1|3|foo}c").mkString(",")).isEqualTo("1c,3c,fooc");
  }

  @Test
  void testMixedTwo() {
    assertThat(Generators.generate("b{1|3|foo}c{4-6}d").mkString(","))
      .isEqualTo("b1c4d,b1c5d,b1c6d,b3c4d,b3c5d,b3c6d,bfooc4d,bfooc5d,bfooc6d");
  }

  @Test
  void testForUrl() {
    assertThat(Generators.generate("https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}")
      .mkString("\n"))
        .isEqualTo("""
            https://legislatie.just.ro/Public/DetaliiDocument/1
            https://legislatie.just.ro/Public/DetaliiDocument/2
            https://legislatie.just.ro/Public/DetaliiDocument/3
            https://legislatie.just.ro/Public/DetaliiDocumentAfis/1
            https://legislatie.just.ro/Public/DetaliiDocumentAfis/2
            https://legislatie.just.ro/Public/DetaliiDocumentAfis/3""");
  }
}
