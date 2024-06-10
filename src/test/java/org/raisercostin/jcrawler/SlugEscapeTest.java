package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.SlugEscape;

class SlugEscapeTest {
  @Test
  void test() {
    String slug = SlugEscape.toSlug(
      "https://posf.ro/comparator/api/index.php?request=comparator-electric&tip_oferta=2&data_start_aplicare=02-06-2024&tip_client=casnic&tip_pret=nediferentiat&consum_anual=1200&consum_lunar=100&valoare_factura_curenta=&nivel_tensiune=JT_&tip_produs=0&perioada_contract=&energie_regenerabila=&factura_electronica=&frecventa_emitere_factura=&procent_zona_noapte=&procent_zona_zi=&frecventa_citire_contor=&valoare_fixa=&denumire_furnizor=&id_zona=8");
    assertThat(slug).isEqualTo(
      "https-----posf--ro--comparator--api--index--php-request=comparator-ele-#8e0719e7b-ctric-tip-oferta=2-data-start-aplic--and-more--sha256-8e0719e7b97f46b0855f9e00932ea6495f227cdb7c5e1cc26ab942e271a56bcb");
  }
}
