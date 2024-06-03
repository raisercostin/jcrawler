package org.raisercostin.jcrawl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import io.vavr.control.Option;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawl.JCrawler.Crawler;
import org.raisercostin.jcrawl.JCrawler.Crawler.TraversalType;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jscraper.JScraper;
import reactor.netty.http.HttpProtocol;

class JCrawlerTest {
  @Test
  //@Disabled
  void testRaisercostin() {
    JCrawler.crawl(Locations.web("raisercostin.org"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan1-raisercostin").mkdirIfNeeded());
  }

  @Test
  void testTheBrainlightOrg() {
    JCrawler.crawl(Locations.web("thebrainlight.org"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan2-brainlight").mkdirIfNeeded());
  }

  @Test
  //@Disabled
  void testRestorcracy() {
    JCrawler.crawl(Locations.web("restocracy.ro"),
      Option.of(Locations.readableFile("d:/home/raiser/work/_var_namek_jcrawl/restocracy-toate-restaurantele.html")),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNeeded());
  }

  @Test
  //@Disabled
  void testRestorcracyScraping() {
    JScraper.scrape(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNeeded());
  }

  Crawler crawler = Crawler
    .crawler()
    .start("https://legislatie.just.ro/Public/DetaliiDocument/1")
    .withCache(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan4-just").mkdirIfNeeded())
    .withFiltersByPrefix(
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/",
      //"https://legislatie.just.ro/Public/DetaliiDocument/1",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/1",
      //"https://legislatie.just.ro/Public/DetaliiDocument/2",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/2",
      //"https://legislatie.just.ro/Public/DetaliiDocument/3",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/3")
    .withCacheExpiryDuration(Duration.ofDays(100))
    .withMaxDocs(5)
    .withMaxConnections(3)
    .withProtocol(HttpProtocol.HTTP11);

  @Test
  //@Disabled
  void testLegeBreadthFirst() {
    //    WebClientLocation2.defaultClient.webclientWireTap.enable();
    //    WebClientLocation2.defaultClient.webclientWireTapType.setRuntimeValue(AdvancedByteBufFormat.HEX_DUMP);
    //TODO add url generators beside scraping?
    //TODO use different httpclients
    //TODO use native java httpclient
    //TODO use virtualThreads
    //TODO rename to RateLimitingDownloader
    RichIterable<String> all = crawler
      .withMaxDocs(1005)
      .withMaxConnections(3)
      //.withCacheExpiryDuration(Duration.ofSeconds(1))
      .withTraversalType(TraversalType.PARALLEL_BREADTH_FIRST)
      //.withGenerator("https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}")
      .withGenerator("https://legislatie.just.ro/Public/DetaliiDocumentAfis/{1000-1010}")
      .crawl();

    String actual = all.sorted().mkString("\n");
    assertThat(actual).isEqualTo(
      """
          https://legislatie.just.ro/Public/DetaliiDocument/1
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/26296
          https://legislatie.just.ro/Public/DetaliiDocumentAfis/129268
          https://legislatie.just.ro/Public/DetaliiDocumentAfis/131085""");
  }

  @Test
  //@Disabled
  void testLegeDepthFirstPreorder() {
    RichIterable<String> all = crawler
      .withTraversalType(TraversalType.DEPTH_FIRST_PREORDER)
      .crawl();

    assertThat(all.mkString("\n")).isEqualTo(
      """
          https://legislatie.just.ro/Public/DetaliiDocument/1
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/132530
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/26296""");
  }

  @Test
  void testMain() {
    //JCrawler.mainOne("https://legislatie.just.ro/Public/DetaliiDocument/1 --debug --protocol=HTTP11");
    JCrawler.mainOne("");
  }

  @Test
  void testAdvisors() {
    JCrawler.crawl(Locations.web("www.holisticadvisors.net/en"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan5-advisors").mkdirIfNeeded());
  }
}
