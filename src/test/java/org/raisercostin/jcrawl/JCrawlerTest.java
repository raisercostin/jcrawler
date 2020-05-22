package org.raisercostin.jcrawl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;

class JCrawlerTest {
  @Test
  @Disabled
  void testRevomatico() {
    JCrawler.crawl(Locations.web("revomatico.com"),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan1-revomatico").mkdirIfNecessary());
  }

  @Test
  @Disabled
  void testRoweb() {
    JCrawler.crawl(Locations.web("roweb.ro"),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan2-roweb").mkdirIfNecessary());
  }

  @Test
  //@Disabled
  void testRestorcracy() {
    JCrawler.crawl(Locations.web("restocracy.ro"),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNecessary());
  }
}
