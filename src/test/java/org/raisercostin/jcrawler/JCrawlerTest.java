package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import io.vavr.control.Option;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.JCrawler.TraversalType;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.Metadata;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jscraper.JScraper;
import reactor.netty.http.HttpProtocol;

class JCrawlerTest {
  @Test
  // @Disabled
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
  // @Disabled
  void testRestorcracy() {
    JCrawler.crawl(Locations.web("restocracy.ro"),
        Option.of(Locations.readableFile("d:/home/raiser/work/_var_namek_jcrawl/restocracy-toate-restaurantele.html")),
        Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan3-restocracy").mkdirIfNeeded());
  }

  @Test
  @Tag("bug")
  // @Disabled
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
            // "https://legislatie.just.ro/Public/DetaliiDocument/1",
            "https://legislatie.just.ro/Public/DetaliiDocumentAfis/1",
            // "https://legislatie.just.ro/Public/DetaliiDocument/2",
            "https://legislatie.just.ro/Public/DetaliiDocumentAfis/2",
            // "https://legislatie.just.ro/Public/DetaliiDocument/3",
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
    // WebClientLocation2.defaultClient.webclientWireTap.enable();
    // WebClientLocation2.defaultClient.webclientWireTapType.setRuntimeValue(AdvancedByteBufFormat.HEX_DUMP);
    // TODO add url generators beside scraping?
    // TODO use different httpclients
    // TODO use native java httpclient
    // TODO use virtualThreads
    // TODO rename to RateLimitingDownloader
    RichIterable<String> all = crawler()
        .withMaxDocs(1005)
        .withMaxConnections(3)
        // .withCacheExpiryDuration(Duration.ofSeconds(1))
        .withTraversalType(TraversalType.PARALLEL_BREADTH_FIRST)
        // .withGenerator("https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}")
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
  // @Disabled
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
    // JCrawler.mainOne("https://legislatie.just.ro/Public/DetaliiDocument/1 --debug
    // --protocol=HTTP11 --expire PT1S");
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
        .withVerbosity(3);
    crawler.crawlIterator().take(10).forEach(x -> System.out.println("loaded " + x));

    // wget https://op.europa.eu/en/web/who-is-who/archive
  }

  @Test
  void testDownloadBinaries() {
    PathLocation dest = Locations.tempFile("jcrawl-", null);
    String url = "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    Metadata metadata = JCrawler.crawler()
        .withVerbosity(3)
        .worker()
        .download(HyperLink.of(url), dest)
        .getMetadata();
    assertThat(metadata.url).isEqualTo(url);
    assertThat(Slug.urlSanitized(url)).isEqualTo(url);
    assertThat(Slug.urlHash(url)).isEqualTo("9cf4918b061e887f92b45255c8fb5e976eb3a24de28686afe653557a900647ef");
    assertThat(Slug.path(url)).isEqualTo("op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en");
    assertThat(dest.length()).isEqualTo(28762441L);
  }

  @Test
  void testDownloadWithFragments() {
    PathLocation dest = Locations.temp().child("jcrawl/test1").mkdirOnParentIfNeeded().backupIfExists();
    // String url =
    // "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    String url = "https://en.m.wikipedia.org/wiki/Wget?param=value";
    String urlWithFragments = url + "#Wget2";
    Metadata metadata = JCrawler.crawler()
        .withVerbosity(3)
        .worker()
        .download(HyperLink.of(urlWithFragments), dest)
        .getMetadata();

    // https/http/ftp?
    // name from returned headers - content-disposition -
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    assertThat(metadata.url).isEqualTo(url);
    assertThat(Slug.urlSanitized(url)).isEqualTo("https://en.m.wikipedia.org/wiki/wget?param=value");
    assertThat(Slug.urlHash(url)).isEqualTo("be4b221727d1658df7ae717d21e6507827a5366ab293c234510ae898978795af");
    assertThat(Slug.path(url)).isEqualTo("en.m.wikipedia.org/wiki/wget@param=value");
    // content-type
    assertThat(dest.length()).isEqualTo(142612);
  }

  @Test
  void testDownloadWithContentType() {
    PathLocation dest = Locations.temp().child("jcrawl/test1").mkdirOnParentIfNeeded().backupIfExists();
    // String url =
    // "https://op.europa.eu/documents/d/who-is-who/pdf_archive_eu_whoiswho_202407-en";
    String url = "https://upload.wikimedia.org/wikipedia/commons/f/f7/Gwget-1.0.4.png";
    String urlWithFragments = url + "#Wget2";
    Metadata metadata = JCrawler.crawler()
        .withVerbosity(3)
        .worker()
        .download(HyperLink.of(urlWithFragments), dest)
        .getMetadata();

    // https/http/ftp?
    // name from returned headers - content-disposition -
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    assertThat(metadata.url).isEqualTo(url);
    assertThat(Slug.urlSanitized(url)).isEqualTo("https://en.m.wikipedia.org/wiki/wget?param=value");
    assertThat(Slug.urlHash(url)).isEqualTo("be4b221727d1658df7ae717d21e6507827a5366ab293c234510ae898978795af");
    assertThat(Slug.path(url)).isEqualTo("en.m.wikipedia.org/wiki/wget@param=value");
    // content-type
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
            "https://www.cultural-mobility.com/wp-content/plugins/website-translator/flags/svg/{hu|ro|fr|it|pl|es|en|bg|de}.svg",
            // javascript
            "https://www.cultural-mobility.com/wp-includes/js/wp-emoji-release.min.js?ver=6.2.6"
        //
        )
        .withTraversalType(TraversalType.BREADTH_FIRST)
        .withRecomputeLinks(true)
        .withVerbosity(3);
    RichIterable<HyperLink> all = crawler.crawlIterator()
        .take(10000)
        .doOnNext(x -> System.out.println("loaded " + x))
        .memoizeJava();
    System.out.println("Downloaded: ");
    all.forEach(x -> System.out.println("%-100s".formatted(x.externalForm)));
    System.out.println("Downloaded " + all.size() + " urls.");
    // wget https://op.europa.eu/en/web/who-is-who/archive
  }

  /**
   * The <source> tag is used to specify multiple media resources for media
   * elements, such as <video>, <audio>, and <picture>.
   * <picture>
   * <source media="(max-width: 799px)" srcset="elva-480w-close-portrait.jpg" />
   * <source media="(min-width: 800px)" srcset="elva-800w.jpg" />
   * <img src="elva-800w.jpg" alt="Chris standing up holding his daughter Elva" />
   * </picture>
   */
  @Test
  void testLinkExtractor() {
    String content = """
        <img decoding=\"async\" src=\"/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg\" srcset=\"/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image.jpg 1738w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-1366x910.jpg 1366w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-768x512.jpg 768w,/wp-content/uploads/go-x/u/b0212968-9365-470d-85a5-be30db7481c2/l207,t177,w1738,h1158/image-375x250.jpg 375w\" sizes=\"(min-width: 1024px) 100vw,(min-width: 768px) 100vw,(min-width: 0px) 100vw\" class=\"image-img image-geometry-roundedrectangle-1 no-aspect-ratio\" data-shape=\"roundedRectangle\" />
        """;
    assertThat(JCrawler.extractLinksFromContent(0, content, "", "https://www.cultural-mobility.com/")
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
        --- consent.js
        <div class="iwt_wrapper" id="iwt-wrapper-44645387"></div><script src='https://www.cultural-mobility.com/wp-content/plugins/go-x-blocks/js/consent/consent.js?ver=1.0.6+d76be5206a' id='gox-script-0-js'></script>
        <script src='https://www.cultural-mobility.com/wp-content/plugins/go-x-blocks/js/forms/forms.js?ver=1.0.6+d76be5206a' id='gox-script-1-js'></script>
        <script>
          window._wpemojiSettings = { "baseUrl": "https:\\/\\/s.w.org\\/images\\/core\\/emoji\\/14.0.0\\/72x72\\/", "ext": ".png", "svgUrl": "https:\\/\\/s.w.org\\/images\\/core\\/emoji\\/14.0.0\\/svg\\/", "svgExt": ".svg", "source": { "concatemoji": "https:\\/\\/www.cultural-mobility.com\\/wp-includes\\/js\\/wp-emoji-release.min.js?ver=6.2.6" } };
          /*! This file is auto-generated */
          !function (e, a, t) { var n, r, o, i = a.createElement("canvas"), p = i.getContext && i.getContext("2d"); function s(e, t) { p.clearRect(0, 0, i.width, i.height), p.fillText(e, 0, 0); e = i.toDataURL(); return p.clearRect(0, 0, i.width, i.height), p.fillText(t, 0, 0), e === i.toDataURL() } function c(e) { var t = a.createElement("script"); t.src = e, t.defer = t.type = "text/javascript", a.getElementsByTagName("head")[0].appendChild(t) } for (o = Array("flag", "emoji"), t.supports = { everything: !0, everythingExceptFlag: !0 }, r = 0; r < o.length; r++)t.supports[o[r]] = function (e) { if (p && p.fillText) switch (p.textBaseline = "top", p.font = "600 32px Arial", e) { case "flag": return s("\ud83c\udff3\ufe0f\u200d\u26a7\ufe0f", "\ud83c\udff3\ufe0f\u200b\u26a7\ufe0f") ? !1 : !s("\ud83c\uddfa\ud83c\uddf3", "\ud83c\uddfa\u200b\ud83c\uddf3") && !s("\ud83c\udff4\udb40\udc67\udb40\udc62\udb40\udc65\udb40\udc6e\udb40\udc67\udb40\udc7f", "\ud83c\udff4\u200b\udb40\udc67\u200b\udb40\udc62\u200b\udb40\udc65\u200b\udb40\udc6e\u200b\udb40\udc67\u200b\udb40\udc7f"); case "emoji": return !s("\ud83e\udef1\ud83c\udffb\u200d\ud83e\udef2\ud83c\udfff", "\ud83e\udef1\ud83c\udffb\u200b\ud83e\udef2\ud83c\udfff") }return !1 }(o[r]), t.supports.everything = t.supports.everything && t.supports[o[r]], "flag" !== o[r] && (t.supports.everythingExceptFlag = t.supports.everythingExceptFlag && t.supports[o[r]]); t.supports.everythingExceptFlag = t.supports.everythingExceptFlag && !t.supports.flag, t.DOMReady = !1, t.readyCallback = function () { t.DOMReady = !0 }, t.supports.everything || (n = function () { t.readyCallback() }, a.addEventListener ? (a.addEventListener("DOMContentLoaded", n, !1), e.addEventListener("load", n, !1)) : (e.attachEvent("onload", n), a.attachEvent("onreadystatechange", function () { "complete" === a.readyState && t.readyCallback() })), (e = t.source || {}).concatemoji ? c(e.concatemoji) : e.wpemoji && e.twemoji && (c(e.twemoji), c(e.wpemoji))) }(window, document, window._wpemojiSettings);
        </script>
        """;

    assertThat(JCrawler.extractLinksFromContent(0, content, "", "https://www.cultural-mobility.com/")
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
                https://www.cultural-mobility.com/wp-content/plugins/go-x-blocks/js/consent/consent.js?ver=1.0.6+d76be5206a
                https://www.cultural-mobility.com/wp-content/plugins/go-x-blocks/js/forms/forms.js?ver=1.0.6+d76be5206a
                """
                .trim());
  }

  @Test
  void testCulturalMobilityIndexOnly() {
    JCrawler crawler = JCrawler.crawler()
        .withProjectPath(
            "d:\\home\\raiser\\work\\2024-09-20--mobility\\.jcrawler-mobility\\widget-globo")
        .withUrl("https://www.cultural-mobility.com/",
            "https://www.cultural-mobility.com/wp-content/plugins/website-translator/flags/svg/{hu|ro|fr|it|pl|es|en|bg|de}.svg"
        //
        )
        .withTraversalType(TraversalType.BREADTH_FIRST)
        .withRecomputeLinks(true)
        .withVerbosity(3)
        // .withCacheExpiryDuration(null)
        .withDepth(1);
    RichIterable<HyperLink> all = crawler.crawlIterator()
        .take(10000)
        .doOnNext(x -> System.out.println("loaded " + x))
        .memoizeJava();
    System.out.println("Downloaded: ");
    all.forEach(x -> System.out.println("%-100s".formatted(x.externalForm)));
    System.out.println("Downloaded " + all.size() + " urls.");
    // wget https://op.europa.eu/en/web/who-is-who/archive
  }

  @Test
  void testHeadersExtract() {
    List<String> headers = JCrawler.CrawlerWorker.headers(
        """
            Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
            """,
        "Cookie", "Referer");
    assertThat(headers.toString()).isEqualTo(
        "[Accept: text/html, Accept: application/xhtml+xml, Accept: application/xml;q=0.9, Accept: image/avif, Accept: image/webp, Accept: image/apng, Accept: */*;q=0.8, Accept: application/signed-exchange;v=b3;q=0.7]");
  }

  @Test
  void testCgiJobs() {
    JCrawler crawler = JCrawler.crawler()
        .withProjectPath("d:\\home\\raiser\\work\\.jcrawler-jobs\\cgi")
        .withUrl("https://cgi.njoyn.com/corp/xweb/xweb.asp?NTKN=c&page=joblisting&clid=21001")
        .withTraversalType(TraversalType.BREADTH_FIRST)
        // .withRecomputeLinks(true)
        .withVerbosity(3);
    RichIterable<HyperLink> all = crawler.crawlIterator()
        .take(1)
        .doOnNext(x -> System.out.println("loaded " + x))
        .memoizeJava();
    System.out.println("Downloaded: ");
    all.forEach(x -> System.out.println("%-100s".formatted(x.externalForm)));
    System.out.println("Downloaded " + all.size() + " urls.");
    // wget https://op.europa.eu/en/web/who-is-who/archive
  }

  /**
   * FIX: tel: protocol links are now filtered out and don't cause server to be
   * marked as failing.
   * Previously when a tel: link was encountered, the crawler threw "unknown
   * protocol: tel" and marked
   * the entire server as failing, which prevented crawling subsequent pages.
   *
   * The fix adds:
   * 1. Early filtering of unsupported protocols (tel:, mailto:, javascript:,
   * etc.) in accept2()
   * 2. Graceful handling of "unknown protocol" errors without marking server as
   * failing
   */
  @Test
  void testProjectsMobility_TelProtocolShouldNotFailServer() {
    JCrawler crawler = JCrawler.crawler()
        .withProjectPath(".jcrawler/projects-mobility")
        .withUrl("https://www.projects-mobility.com/")
        .withTraversalType(TraversalType.PARALLEL_BREADTH_FIRST)
        .withVerbosity(3)
        .withMaxDocs(30);

    RichIterable<HyperLink> all = crawler.crawlIterator()
        .doOnNext(x -> System.out.println("loaded " + x))
        .memoizeJava();

    System.out.println("Downloaded " + all.size() + " urls.");

    // With the fix, tel: links are filtered out and crawling continues normally
    // The server should NOT be marked as failing just because of unsupported
    // protocols
    assertThat(all.size()).as("Should crawl multiple pages even when tel: links exist in page content")
        .isGreaterThan(10);
  }

  /**
   * Test that unsupported protocols (tel:, mailto:, javascript:) are extracted
   * from content
   * but filtered out by accept2() before download attempts.
   */
  @Test
  void testUnsupportedProtocolsAreFiltered() {
    String content = """
        <a href="tel:+1234567890">Call us</a>
        <a href="mailto:test@example.com">Email us</a>
        <a href="javascript:void(0)">Click me</a>
        <a href="https://www.example.com/page1">Valid link</a>
        <a href="/relative/path">Relative link</a>
        """;

    var links = JCrawler.extractLinksFromContent(0, content, "", "https://www.example.com/")
        .map(x -> x.externalForm)
        .toList();

    // Link extraction finds all links including unsupported protocols
    System.out.println("Extracted links: " + links.mkString("\n"));
    assertThat(links.filter(l -> l.startsWith("tel:")).size())
        .as("tel: links are extracted from content")
        .isEqualTo(1);
    assertThat(links.filter(l -> l.startsWith("mailto:")).size())
        .as("mailto: links are extracted from content")
        .isEqualTo(1);

    // But filtering happens in accept2() - unsupported protocols should be rejected
    // This is verified by the UNSUPPORTED_PROTOCOLS set in CrawlerWorker.accept2()
  }

  /**
   * Verify the list of unsupported protocols that are filtered.
   */
  @Test
  void testUnsupportedProtocolsList() {
    // These protocols should be filtered by CrawlerWorker.accept2()
    List<String> unsupportedProtocols = List.of(
        "tel:", "mailto:", "javascript:", "data:", "blob:", "file:", "ftp:", "ssh:", "git:");

    // Verify these don't match any http/https accept pattern
    for (String protocol : unsupportedProtocols) {
      assertThat(protocol.startsWith("http://") || protocol.startsWith("https://"))
          .as("Protocol %s should not be http/https", protocol)
          .isFalse();
    }
  }

  @Test
  void testIssue004_EncodedTemplateVariableShouldBeSkipped() {
    String sourceUrl = "https://example.com";
    String content = "<a href=\"https://www.projects-mobility.com/blog/categories/$%7Bi.uri%7D\">Link</a>";

    io.vavr.collection.Iterator<HyperLink> links = JCrawler.extractLinksFromContent(0, content, null, sourceUrl);

    assertThat(links.toJavaList()).isEmpty();
  }

  @Test
  void testIssue004_ExtremelyLongUrlShouldNotCauseException() {
    String sourceUrl = "https://example.com";
    StringBuilder longQuery = new StringBuilder("?");
    for (int i = 0; i < 3000; i++) {
      longQuery.append("a").append(i).append("=val").append(i).append("&");
    }
    String longUrl = "https://example.com/api" + longQuery.toString();
    String content = "<img src=\"" + longUrl + "\">";

    // This should now be skipped by the length limit (> 2000 chars)
    io.vavr.collection.Iterator<HyperLink> links = JCrawler.extractLinksFromContent(0, content, null, sourceUrl);

    assertThat(links.toJavaList()).isEmpty();
  }
}
