package org.raisercostin.jcrawl;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;

class JCrawlerTest {
  @Test
  void test() {
    //    JCrawler.crawl(Locations.web("restocracy.ro"),
    //      Locations.dir("d:\\home\\raiser\\work\\_var_restocracy\\scan2").mkdirIfNecessary());
  }

  @Test
  void testRevomaticoStatic() {
    JCrawler.crawl(Locations.web("revomatico.com"),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan1").mkdirIfNecessary());
  }
}
