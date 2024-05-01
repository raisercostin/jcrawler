package org.raisercostin.jcrawl;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Option;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawl.JCrawler.CrawlConfig;
import org.raisercostin.jcrawl.JCrawler.TraversalType;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.url.WebClientLocation2;
import org.raisercostin.jedio.url.WebClientLocation2.WebClientFactory;
import org.raisercostin.jscraper.JScraper;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

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

  CrawlConfig config = CrawlConfig
    .start("https://legislatie.just.ro/Public/DetaliiDocument/1")
    .withCache(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan4-just").mkdirIfNeeded())
    .withFiltersByPrefix(
      "https://legislatie.just.ro/Public/DetaliiDocument/1",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/1",
      "https://legislatie.just.ro/Public/DetaliiDocument/2")
    .withMaxDocs(5)
    .withMaxConnections(5)
    .withProtocol(HttpProtocol.HTTP11);

  @Test
  //@Disabled
  void testLegeBreadthFirst() {
    //    WebClientLocation2.defaultClient.webclientWireTap.enable();
    //    WebClientLocation2.defaultClient.webclientWireTapType.setRuntimeValue(AdvancedByteBufFormat.HEX_DUMP);
    //TODO add cache on disk - disk io is faster than network io
    //TODO add parallelism - ParallelTraverser?
    //TODO use different httpclients
    //TODO use native java httpclient
    //TODO use virtualThreads
    //TODO rename to RateLimitingDownloader
    RichIterable<String> all = JCrawler.crawl(
      config
        .withTraversalType(TraversalType.BREADTH_FIRST)
    //
    );

    assertThat(all.mkString("\n")).isEqualTo(
      """
          https://legislatie.just.ro/Public/DetaliiDocument/1
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/26296
          https://legislatie.just.ro/Public/DetaliiDocumentAfis/131085
          https://legislatie.just.ro/Public/DetaliiDocumentAfis/129268""");
  }

  @Test
  //@Disabled
  void testLegeDepthFirstPreorder() {
    RichIterable<String> all = JCrawler.crawl(
      config
        .withTraversalType(TraversalType.DEPTH_FIRST_PREORDER)
    //
    );

    assertThat(all.mkString("\n")).isEqualTo(
      """
          https://legislatie.just.ro/Public/DetaliiDocument/1
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/132530
          https://legislatie.just.ro/Public/DetaliiDocument/131185
          https://legislatie.just.ro/Public/DetaliiDocument/26296""");
  }
}
