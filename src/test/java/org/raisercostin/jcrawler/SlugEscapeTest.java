package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugEscapeTest {
  @Test
  void test() {
    String slug = Slug.contentPathInitial(
      "https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8").slug;
    //    assertThat(slug).isEqualTo(
    //        "posf.ro/comparator/api/index.php@request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=jt_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8.tmp2");
    assertThat(slug).isEqualTo(
      "posf.ro/comparator/api/index.php.tmp2");
  }

  @Test
  void test2() {
    String slug = Slug.contentPathInitial(
      "https://www.cultural-mobility.com/wp-json/oembed/1.0/embed?url=https%253A%252F%252Fwww.cultural-mobility.com%252F").slug;
    //    assertThat(slug).isEqualTo(
    //      //"www.cultural-mobility.com/wp-json/oembed/1.0/embed@url=https://www.cultural-mobility.com//index.html.tmp2"
    //      "www.cultural-mobility.com/wp-json/oembed/1.0/embed@url=https//www.cultural-mobility.com//index.html.tmp2");
    assertThat(slug).isEqualTo(
      //"www.cultural-mobility.com/wp-json/oembed/1.0/embed@url=https://www.cultural-mobility.com//index.html.tmp2"
      "www.cultural-mobility.com/wp-json/oembed/1.0/embed.tmp2");
  }

  @Test
  void testCollisions() {
    String url1 = "https://raisercostin.org/2017/02/08/define%20risk%20takers";
    String url2 = "https://raisercostin.org/2017/02/08/define%20safety%20nets";

    assertThat(Slug.contentPathInitial(url1).slug)
      .isEqualTo("raisercostin.org/2017/02/08/define risk takers.tmp2");
    assertThat(Slug.contentPathInitial(url2).slug)
      .isEqualTo("raisercostin.org/2017/02/08/define safety nets.tmp2");
    //-> file://localhost/D:/home/raiser/work/namek-jcrawl/.crawl/https-----raisercostin--org--2017--02--08--safety-net-#081b642dc---sha256-081b642dc18241e29deb20b1177f6fae437f5b9b9b77aa672f740501788bdfd6
  }
}
