package org.raisercostin.jscraper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;

@Slf4j
class JScraperTest2 {
  @Test
  void testCulturalMobility() {
    String crawledDir = "d:\\home\\raiser\\work\\2024-09-20--mobility\\.jcrawler-mobility";
    JScraper.scrape(Locations.path(crawledDir).child("www.cultural-mobility.com/about-us/"));
    JScraper.scrape(Locations.path(crawledDir).child("www.cultural-mobility.com/courses-for-teachers/"));
    //src='https://www.cultural-mobility.com/wp-content/plugins/go-x-blocks/js/consent/consent.js?ver=1.0.6+d76be5206a'
  }
}
