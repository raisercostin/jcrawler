package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.JCrawler.CrawlerWorker;

class PageResourcesTest {

        @Test
        void testResourcesAreAcceptedFromExternalDomains() {
                // 1. Setup Crawler for a specific domain
                JCrawler config = JCrawler.crawler()
                                .withUrl("https://mysite.com");

                CrawlerWorker worker = new CrawlerWorker(config);

                String source = "https://mysite.com/index.html";
                String cacheKey = "cache";

                // 2. Define links
                // Internal Navigation
                HyperLink internalNav = HyperLink.of("https://mysite.com/page1", 1, "Next Page", "", "", source,
                                cacheKey,
                                false, false);

                // External Navigation (should be rejected)
                HyperLink externalNav = HyperLink.of("https://other.com/page", 1, "External Link", "", "", source,
                                cacheKey,
                                false, false);

                // External Resource (Image) (should be accepted)
                HyperLink externalImg = HyperLink.of("https://cdn.external.com/logo.png", 1, "Logo", "", "", source,
                                cacheKey,
                                false, true);

                // External Resource (Script) (should be accepted)
                HyperLink externalScript = HyperLink.of("https://apis.google.com/js/api.js", 1, "Script", "", "",
                                source,
                                cacheKey, false, true);

                // External Link to PDF (should be recognized as resource by extension)
                // Note: we pass isResource=false here to simulate that regex didn't catch it,
                // but the internal logic in JCrawler should upgrade it to true based on
                // extension.
                // However, HyperLink itself is a data holder. The logic is in
                // JCrawler.extractLinksFromContent.
                // *Testing Strategy*:
                // Since we are unit testing `CrawlerWorker.accept()`, and `accept()` relies on
                // the `HyperLink` object
                // already having `isResource=true` (which is set during extraction), we
                // strictly need to test
                // if `accept()` respects `isResource`.
                //
                // BUT, to verify the "extension" logic, we usually need an Integration Test on
                // extraction.
                // For this unit test, we will assume Extraction worked and passed TRUE for the
                // PDF.
                HyperLink externalPdf = HyperLink.of("https://documents.com/guide.pdf", 1, "PDF Guide", "", "", source,
                                cacheKey, false, true);

                // 3. Test Accept Logic
                assertThat(worker.accept(internalNav)).as("Internal navigation should be accepted").isTrue();

                assertThat(worker.accept(externalNav)).as("External navigation should be rejected").isFalse();
                assertThat(config.ignoredExternalDomains).contains("other.com");

                assertThat(worker.accept(externalImg)).as("External resource (image) should be accepted").isTrue();
                assertThat(config.ignoredExternalDomains).doesNotContain("cdn.external.com"); // Accepted links
                                                                                              // shouldn't be in
                                                                                              // ignored

                assertThat(worker.accept(externalScript)).as("External resource (script) should be accepted").isTrue();
                assertThat(config.ignoredExternalDomains).doesNotContain("apis.google.com");

                // PDF check
                assertThat(worker.accept(externalPdf))
                                .as("External PDF should be accepted as resource due to extension").isTrue();
                assertThat(config.ignoredExternalDomains).doesNotContain("documents.com");

                // 4. Test Redirect Logic
                // Scenario: Internal link redirects to external URL. The crawler flags this as
                // isRedirect=true.
                HyperLink redirectLink = HyperLink.of("https://redirected-site.com/page", 1, "Redirected", "", "",
                                source, cacheKey, true, false);

                assertThat(worker.accept(redirectLink))
                                .as("External redirect target should be accepted (ownership implied)").isTrue();
                assertThat(config.ignoredExternalDomains).doesNotContain("redirected-site.com");
        }
}
