package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jcrawler.JCrawler.CrawlerWorker;

class ExternalDomainTest {

    @Test
    void testIgnoredExternalDomainsAreTracked() {
        // 1. Setup Crawler for a specific domain
        JCrawler config = JCrawler.crawler()
                .withUrl("https://mysite.com"); // Only mysite.com is accepted by default logic usually, or depends on
                                                // defaults.

        // Ensure we restrict to hostname if not already default
        // In JCrawler constructor, accessHostname might be set.
        // Let's rely on standard constructor initialization.

        CrawlerWorker worker = new CrawlerWorker(config);

        // 2. Define links
        HyperLink internalLink = HyperLink.of("https://mysite.com/page1");
        HyperLink externalLink = HyperLink.of("https://wixstatic.com/image.png");
        HyperLink anotherExternal = HyperLink.of("http://other-site.org/foo");
        HyperLink unsupported = HyperLink.of("mailto:user@mysite.com");

        // 3. Test Accept Logic
        // Internal should be accepted (assuming depth is fine, default is 100)
        assertThat(worker.accept(internalLink)).as("Internal link should be accepted").isTrue();

        // External should be rejected
        assertThat(worker.accept(externalLink)).as("External link should be rejected").isFalse();

        // Check if it was added to ignoredExternalDomains
        assertThat(config.ignoredExternalDomains).contains("wixstatic.com");

        // Another external
        assertThat(worker.accept(anotherExternal)).as("Another external link should be rejected").isFalse();
        assertThat(config.ignoredExternalDomains).contains("other-site.org");

        // Unsupported protocol
        assertThat(worker.accept(unsupported)).as("Mailto should be rejected").isFalse();
        // Should NOT be added to domains (it doesn't have a hostname really, or logic
        // excludes it)
        // "mailto:..." URL might parse hostname as null or empty, or logic explicitly
        // excludes protocols BEFORE adding.
        // My implementation:
        // for (String protocol : UNSUPPORTED_PROTOCOLS) if (url.startsWith(protocol))
        // unsupported = true;
        // if (!unsupported) { add ... }
        assertThat(config.ignoredExternalDomains).doesNotContain("user@mysite.com");
        assertThat(config.ignoredExternalDomains).doesNotContain("mailto");
    }
}
