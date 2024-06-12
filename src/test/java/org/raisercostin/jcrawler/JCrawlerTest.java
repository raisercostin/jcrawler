package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import io.vavr.control.Option;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.JCrawler;
import org.raisercostin.jcrawler.JCrawler.TraversalType;
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
  @Tag("bug")
  //@Disabled
  void testRestorcracyScraping() {
    JScraper.scrape(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNeeded());
  }

  JCrawler crawler = JCrawler
    .crawler()
    .withUrl("https://legislatie.just.ro/Public/DetaliiDocument/1")
    .withProjectPath(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan4-just").mkdirIfNeeded())
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
  @Tag("bug-freeze")
  @Disabled
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
      .withUrl("https://legislatie.just.ro/Public/DetaliiDocumentAfis/{1000-1010}")
      .crawl()
      .map(x -> x.externalForm);

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
  @Tag("bug")
  //@Disabled
  void testLegeDepthFirstPreorder() {
    RichIterable<String> all = crawler
      .withTraversalType(TraversalType.DEPTH_FIRST_PREORDER)
      .crawl()
      .map(x -> x.externalForm);

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
    //JCrawler.mainOne("https://legislatie.just.ro/Public/DetaliiDocument/1 --debug --protocol=HTTP11 --expire PT1S");
    JCrawler.mainOne("https://raisercostin.org --traversal=BREADTH_FIRST", false);
  }

  @Test
  void testAdvisors() {
    JCrawler.crawl(Locations.web("www.holisticadvisors.net/en"), Option.none(),
      Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan5-advisors").mkdirIfNeeded());
  }

  @Test
  void raisercostinOrg() {
    JCrawler.mainOne("", false);
    JCrawler crawler = JCrawler.crawler().withUrl("http://raisercostin.org");
    assertThat(crawler.crawl().take(6).mkString("\n")).isEqualTo("""
        http://raisercostin.org
        https://raisercostin.org/
        https://raisercostin.org/bliki
        https://raisercostin.org/talk
        https://raisercostin.org/2017/04/24/GhostInTheShell
        https://raisercostin.org/2017/04/18/PalidulAlbastruPunct""");
  }
}
