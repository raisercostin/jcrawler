package org.raisercostin.jcrawl;

import io.vavr.control.Option;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawl.JCrawler.CrawlConfig;
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

  @Test
  //@Disabled
  void testLege() {
    //    WebClientLocation2.defaultClient.webclientWireTap.enable();
    //    WebClientLocation2.defaultClient.webclientWireTapType.setRuntimeValue(AdvancedByteBufFormat.HEX_DUMP);

    //TODO use different httpclients
    //TODO use native java httpclient
    //TODO use virtualThreads
    //Configure http11 protocol or 2
    //  HttpClient httpClient = HttpClient
    //  .create(provider)
    //  .protocol(HttpProtocol.HTTP11)
    //  //.responseTimeout(Duration.ofSeconds(10))
    //  .compress(true)
    //WebClientLocation2.defaultClient.builder.clientConnector(new ReactorClientHttpConnector(httpClient));
    //TODO rename to RateLimitingDownloader
    JCrawler.crawl(
      CrawlConfig
        .start("https://legislatie.just.ro/Public/DetaliiDocument/1")
        .withCache(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan4-just").mkdirIfNeeded())
        .withFilters("https://legislatie.just.ro/Public/DetaliiDocument/1",
          "https://legislatie.just.ro/Public/DetaliiDocumentAfis/1",
          "https://legislatie.just.ro/Public/DetaliiDocument/2")
        .withMaxDocs(5)
    //
    );
    //add maxDocs:100, parallelism:5
  }
}
