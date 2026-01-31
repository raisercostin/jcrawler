package org.raisercostin.jcrawler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class SlugTest {

    @Test
    public void testSlugTransformationOfDataUri() {
        String dataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAUoAAAAaCAYAAADR0BVG";
        String path = Slug.path(dataUri, true);

        // Slug.path replaces ':' with '\uF03A' (Private Use Area)
        assertThat(path).contains("\uF03A");
        assertThat(path).startsWith("data\uF03Aimage/png;base64,");
    }

    @Test
    public void testSlugUrlSanitizedDoesNotTrim() {
        // This test documents that Slug.urlSanitized does NOT trim by design (or
        // current implementation)
        // This justifies why JCrawler must trim the URLs before passing them to Slug
        String urlWithSpace = " http://example.com ";
        String sanitized = Slug.urlSanitized(urlWithSpace);
        assertThat(sanitized).isEqualTo(" http://example.com ");
    }
}
