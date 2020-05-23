package org.raisercostin.jcrawl;

import io.vavr.control.Option;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jscraper.JScraper;

class JCrawlerTest {
  @Test
  @Disabled
  void testRevomatico() {
    JCrawler.crawl(Locations.web("revomatico.com"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan1-revomatico").mkdirIfNecessary());
  }

  @Test
  @Disabled
  void testRoweb() {
    JCrawler.crawl(Locations.web("roweb.ro"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan2-roweb").mkdirIfNecessary());
  }

  @Test
  //@Disabled
  void testRestorcracy() {
    JCrawler.crawl(Locations.web("restocracy.ro"),
      Option.of(Locations.readableFile("d:/home/raiser/work/_var_namek_jcrawl/restocracy-toate-restaurantele.html")),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNecessary());
  }

  @Test
  //@Disabled
  void testRestorcracyScraping() {
    JScraper.scrape(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNecessary());
  }
}
