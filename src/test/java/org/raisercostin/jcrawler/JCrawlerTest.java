package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import io.vavr.control.Option;
import org.jedio.regex.RichRegex;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.JCrawler;
import org.raisercostin.jcrawler.JCrawler.TraversalType;
import org.raisercostin.jcrawler.JCrawler.Verbosity;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.Metadata;
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

  JCrawler crawler() {
    return JCrawler
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
  }

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
    RichIterable<String> all = crawler()
      .withMaxDocs(1005)
      .withMaxConnections(3)
      //.withCacheExpiryDuration(Duration.ofSeconds(1))
      .withTraversalType(TraversalType.PARALLEL_BREADTH_FIRST)
      //.withGenerator("https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}")
      .withUrl("https://legislatie.just.ro/Public/DetaliiDocumentAfis/{1000-1010}")
      .crawlIterator()
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
    RichIterable<String> all = crawler()
      .withTraversalType(TraversalType.DEPTH_FIRST_PREORDER)
      .crawlIterator()
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
    assertThat(crawler.crawlIterator().take(6).mkString("\n")).isEqualTo("""
        http://raisercostin.org
        https://raisercostin.org/
        https://raisercostin.org/bliki
        https://raisercostin.org/talk
        https://raisercostin.org/2017/04/24/GhostInTheShell
        https://raisercostin.org/2017/04/18/PalidulAlbastruPunct""");
  }

  @Test
  void testWhoIsWho() {
    JCrawler crawler = JCrawler.crawler()
      .withProjectPath(".jcrawler/whoiswho")
      .withUrl("https://op.europa.eu/en/web/who-is-who/archive")
      .withTraversalType(TraversalType.BREADTH_FIRST)
      .withVerbosity(Verbosity.DEBUG);
    crawler.crawlIterator().take(10).forEach(x -> System.out.println("loaded " + x));

    //wget https://op.europa.eu/en/web/who-is-who/archive
  }

  @Test
  void testDownloadBinaries() {
    PathLocation dest = Locations.tempFile("jcrawl-", null);
    String url = "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    Metadata metadata = JCrawler.crawler()
      .withVerbosity(Verbosity.DEBUG)
      .worker()
      .download(url, dest)
      .getMetadata();
    assertThat(metadata.url).isEqualTo(url);
    assertThat(metadata.urlSanitized()).isEqualTo(url);
    assertThat(metadata.urlHash()).isEqualTo("9cf4918b061e887f92b45255c8fb5e976eb3a24de28686afe653557a900647ef");
    assertThat(metadata.path()).isEqualTo("op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en");
    assertThat(dest.length()).isEqualTo(28762441L);
  }

  @Test
  void testDownloadWithFragments() {
    PathLocation dest = Locations.temp().child("jcrawl/test1").mkdirOnParentIfNeeded().backupIfExists();
    //String url = "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    String url = "https://en.m.wikipedia.org/wiki/Wget?param=value";
    String urlWithFragments = url + "#Wget2";
    Metadata metadata = JCrawler.crawler()
      .withVerbosity(Verbosity.DEBUG)
      .worker()
      .download(urlWithFragments, dest)
      .getMetadata();

    //https/http/ftp?
    //name from returned headers - content-disposition - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    assertThat(metadata.url).isEqualTo(url);
    assertThat(metadata.urlSanitized()).isEqualTo("https://en.m.wikipedia.org/wiki/wget?param=value");
    assertThat(metadata.urlHash()).isEqualTo("be4b221727d1658df7ae717d21e6507827a5366ab293c234510ae898978795af");
    assertThat(metadata.path()).isEqualTo("en.m.wikipedia.org/wiki/wget@param=value");
    //content-type
    assertThat(dest.length()).isEqualTo(142612);
  }

  @Test
  void testDownloadWithContentType() {
    PathLocation dest = Locations.temp().child("jcrawl/test1").mkdirOnParentIfNeeded().backupIfExists();
    //String url = "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    String url = "https://upload.wikimedia.org/wikipedia/commons/f/f7/Gwget-1.0.4.png";
    String urlWithFragments = url + "#Wget2";
    Metadata metadata = JCrawler.crawler()
      .withVerbosity(Verbosity.DEBUG)
      .worker()
      .download(urlWithFragments, dest)
      .getMetadata();

    //https/http/ftp?
    //name from returned headers - content-disposition - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    assertThat(metadata.url).isEqualTo(url);
    assertThat(metadata.urlSanitized()).isEqualTo("https://en.m.wikipedia.org/wiki/wget?param=value");
    assertThat(metadata.urlHash()).isEqualTo("be4b221727d1658df7ae717d21e6507827a5366ab293c234510ae898978795af");
    assertThat(metadata.path()).isEqualTo("en.m.wikipedia.org/wiki/wget@param=value");
    assertThat(metadata.path()).isEqualTo("en.m.wikipedia.org/wiki/wget@param=value");
    //content-type
    assertThat(dest.length()).isEqualTo(142612);
  }

  @Test
  void testCulturalMobility() {
    JCrawler crawler = JCrawler.crawler()
      .withProjectPath("d:\\home\\raiser\\work\\2024-09-20--mobility\\.jcrawler-mobility")
      .withUrl("https://cultural-mobility.com",
        "https://www.cultural-mobility.com/robots.txt",
        "https://www.cultural-mobility.com/sitemap.xml",
        "https://www.cultural-mobility.com/sitemap.xml.gz",
        "https://www.cultural-mobility.com/sitemap.gz",
        "https://www.cultural-mobility.com/favicon.ico",
        "https://www.cultural-mobility.com/robots.txt",
        "https://www.cultural-mobility.com/wp-content/themes/gox/public/legal/maps/es-ES.html",
        "https://www.cultural-mobility.com/wp-content/themes/gox/public/legal/websiteTranslator/es-ES.html",
        "https://www.cultural-mobility.com/wp-content/plugins/website-translator/flags/svg/{hu|ro|fr|it|pl|es|en|bg|de}.svg")
      .withTraversalType(TraversalType.BREADTH_FIRST)
      .withRecomputeLinks(true)
      .withVerbosity(Verbosity.DEBUG);
    RichIterable<HyperLink> all = crawler.crawlIterator()
      .take(10000)
      .doOnNext(x -> System.out.println("loaded " + x))
      .memoizeJava();
    System.out.println("Downloaded: ");
    all.forEach(x -> System.out.println("%-100s".formatted(x.externalForm)));
    System.out.println("Downloaded " + all.size() + " urls.");
    //wget https://op.europa.eu/en/web/who-is-who/archive
  }

  /**
  The <source> tag is used to specify multiple media resources for media elements, such as <video>, <audio>, and <picture>.
  <picture>
  <source media="(max-width: 799px)" srcset="elva-480w-close-portrait.jpg" />
  <source media="(min-width: 800px)" srcset="elva-800w.jpg" />
  <img src="elva-800w.jpg" alt="Chris standing up holding his daughter Elva" />
  </picture>
   */
  @Test
  void testLinkExtractor() {
    String contentUrls = """
        <img decoding=\"async\" src=\"/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg\"
        srcset=\"
        /wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg 1738w,
        /wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-1366x910.jpg 1366w,
        /wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-768x512.jpg 768w,
        /wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-375x250.jpg 375w
        \" sizes=\"(min-width: 1024px) 100vw,(min-width: 768px) 100vw,(min-width: 0px) 100vw\" class=\"image-img image-geometry-roundedrectangle-1 no-aspect-ratio\" data-shape=\"roundedRectangle\" />
        """;

    String content = """
        <img decoding=\"async\" src=\"/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg\" srcset=\"/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg 1738w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-1366x910.jpg 1366w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-768x512.jpg 768w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-375x250.jpg 375w\" sizes=\"(min-width: 1024px) 100vw,(min-width: 768px) 100vw,(min-width: 0px) 100vw\" class=\"image-img image-geometry-roundedrectangle-1 no-aspect-ratio\" data-shape=\"roundedRectangle\" />
        """;
    assertThat(JCrawler.extractLinksFromContent(content, "", "https://www.cultural-mobility.com/")
      .map(x -> x.externalForm)
      .mkString("\n")).isEqualTo(
        """
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg
            https://www.cultural-mobility.com/,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-1366x910.jpg
            https://www.cultural-mobility.com/,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-768x512.jpg
            https://www.cultural-mobility.com/,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-375x250.jpg""");
  }

  @Test
  void testLinkExtractor2() {
    String content = """
        <img decoding="async" src="/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image.jpg" srcset="/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image.jpg 427w,/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-320x1494.jpg 320w,/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-228x1064.jpg 228w,/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-256x1195.jpg 256w,/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-375x1750.jpg 375w" sizes="(min-width: 1024px) 17vw,(min-width: 768px) 33vw,(min-width: 0px) 100vw" class="image-img image-geometry-rectangle-1 no-aspect-ratio" data-shape="rectangle">
        <link rel="shortcut icon" href="/wp-content/uploads/go-x/u/fd3bbf89-20d4-4067-b378-11070f9cb39e/w16,h16,rtfit,bg,el1,ex1,fico/image.ico?v=1726218050425" type="image/x-icon" />
        --- robots.txt
        User-agent: *
        Disallow: /wp-admin/
        Allow: /wp-admin/admin-ajax.php

        Sitemap: https://www.cultural-mobility.com/wp-sitemap.xml
        ---- wp-sitemap.xml
        <?xml version="1.0" encoding="UTF-8"?>
        <?xml-stylesheet type="text/xsl" href="https://www.cultural-mobility.com/wp-sitemap-index.xsl" ?>
        <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><sitemap><loc>https://www.cultural-mobility.com/wp-sitemap-posts-page-1.xml</loc></sitemap></sitemapindex>
        --- wp-sitemap-posts-page-1.xml
        <?xml version="1.0" encoding="UTF-8"?>
        <?xml-stylesheet type="text/xsl" href="https://www.cultural-mobility.com/wp-sitemap.xsl" ?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>https://www.cultural-mobility.com/</loc><lastmod>2024-09-13T09:00:59+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/services/</loc><lastmod>2024-09-13T09:00:59+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/about-us/</loc><lastmod>2024-09-13T09:00:59+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/get-in-touch/</loc><lastmod>2024-09-13T09:00:59+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/erasmus-mobilities/</loc><lastmod>2024-09-13T09:00:59+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/services/transport/</loc><lastmod>2024-09-13T09:01:00+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/courses-for-teachers/</loc><lastmod>2024-09-13T09:01:00+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/erasmus-mobilities/job-shadowing/</loc><lastmod>2024-09-13T09:01:00+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/services/accommodations-and-meals/</loc><lastmod>2024-09-13T09:01:00+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/erasmus-mobilities/vet-internships/</loc><lastmod>2024-09-13T09:01:00+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/erasmus-mobilities/school-education/</loc><lastmod>2024-09-13T09:01:01+00:00</lastmod><changefreq>daily</changefreq></url><url><loc>https://www.cultural-mobility.com/services/cultural-visits-and-other-activities/</loc><lastmod>2024-09-13T09:01:01+00:00</lastmod><changefreq>daily</changefreq></url></urlset>
        """;

    assertThat(JCrawler.extractLinksFromContent(content, "", "https://www.cultural-mobility.com/")
      .map(x -> x.externalForm)
      .mkString("\n")).isEqualTo(
        """
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-320x1494.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-228x1064.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-256x1195.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/c48e3c26-1930-4664-86d2-ce59dd110b57/l468,t0,w427,h1993/image-375x1750.jpg
            https://www.cultural-mobility.com/wp-content/uploads/go-x/u/fd3bbf89-20d4-4067-b378-11070f9cb39e/w16,h16,rtfit,bg,el1,ex1,fico/image.ico?v=1726218050425
            https://www.cultural-mobility.com/wp-admin/
            https://www.cultural-mobility.com/wp-admin/admin-ajax.php
            https://www.cultural-mobility.com/wp-sitemap.xml
            https://www.cultural-mobility.com/wp-sitemap-index.xsl
            https://www.cultural-mobility.com/wp-sitemap.xsl
            https://www.cultural-mobility.com/wp-sitemap-posts-page-1.xml
            https://www.cultural-mobility.com/
            https://www.cultural-mobility.com/services/
            https://www.cultural-mobility.com/about-us/
            https://www.cultural-mobility.com/get-in-touch/
            https://www.cultural-mobility.com/erasmus-mobilities/
            https://www.cultural-mobility.com/services/transport/
            https://www.cultural-mobility.com/courses-for-teachers/
            https://www.cultural-mobility.com/erasmus-mobilities/job-shadowing/
            https://www.cultural-mobility.com/services/accommodations-and-meals/
            https://www.cultural-mobility.com/erasmus-mobilities/vet-internships/
            https://www.cultural-mobility.com/erasmus-mobilities/school-education/
            https://www.cultural-mobility.com/services/cultural-visits-and-other-activities/
            """
          .trim());
  }
}
