package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.SlugEscape;

class SlugEscapeTest {
  @Test
  void test() {
    String slug = SlugEscape.toSlug(
      "https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8").slug;
    assertThat(slug).isEqualTo(
      "https-----posf--ro--comparator--api--index--php-request=comparator-ele-#8e0719e7b-ctric-tip-oferta=2-data-start-aplic--and-more--sha256-8e0719e7b97f46b0855f9e00932ea6495f227cdb7c5e1cc26ab942e271a56bcb");
  }

  @Test
  void testCollisions() {
    String url1 = "https://raisercostin.org/2017/02/08/define%20risk%20takers";
    String url2 = "https://raisercostin.org/2017/02/08/define%20safety%20nets";

    assertThat(SlugEscape.toSlug(url1)).isEqualTo(
      "https-----raisercostin--org--2017--02--08--define-risk-takers-#e8fde51af---sha256-e8fde51af75c740204f26ee5a0407a995f92f957387aa7423e7d4f25d2a0433d");
    assertThat(SlugEscape.toSlug(url2)).isEqualTo(
      "https-----raisercostin--org--2017--02--08--define-safety-nets-#84dfdf4e6---sha256-84dfdf4e6d89272362d371c8d917fa2b179c96cd7525b77391392a06e9a84433");
    //-> file://localhost/D:/home/raiser/work/namek-jcrawl/.crawl/https-----raisercostin--org--2017--02--08--safety-net-#081b642dc---sha256-081b642dc18241e29deb20b1177f6fae437f5b9b9b77aa672f740501788bdfd6
  }
}
