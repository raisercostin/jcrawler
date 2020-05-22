package org.raisercostin.jscraper;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;

class JScraperTest {
  @Test
  void test() {
    JScraper.scrape(Locations.existingDir(
      "d:/home/raiser/work/_var_namek_jcrawl/scan3-restocracy/ro_restocracy_www--https--acuarela-bistro-restaurant--"));
  }
}
