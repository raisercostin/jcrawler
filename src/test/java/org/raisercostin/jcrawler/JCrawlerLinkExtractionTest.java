package org.raisercostin.jcrawler;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class JCrawlerLinkExtractionTest {

    @Test
    public void testExtractLinksSkipsDataUri() {
        String html = "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAU\">";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testExtractLinksSkipsEncodedDataUri() {
        String html = "<img src=\"data%3Aimage/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAU\">";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testExtractLinksSkipsTemplateVariable() {
        String html = "<a href=\"https://example.com/blog/${i.uri}\">Link</a>";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testExtractLinksSkipsEncodedTemplateVariable() {
        String html = "<a href=\"https://example.com/blog/$%7Bi.uri%7D\">Link</a>";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testDataUriInSrcsetCausesPathException() {
        String html = """
                <img srcset="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAADwSURBVDhPY2AYBaNgFIwCqgEGBgYGJiYmBkZGRgZmZmYGFhYWBjY2NgZ2dnYGDg4OBk5OTgYuLi4Gbm5uBh4eHgZeXl4GPj4+Bn5+fgYBAQEGQUFBBiEhIQZhYWEGERERBlFRUQYxMTEGcXFxBgkJCQZJSUkGKSkpBmlpaQYZGRkGWVlZBjk5OQZ5eXkGBQUFBkVFRQYlJSUGZWVlBhUVFQZVVVUGNTU1BnV1dQYNDQ0GTU1NBi0tLQZtbW0GHR0dBl1dXQY9PT0GfX19BgMDAwZDQ0MGIyMjBmNjYwYTExMGU1NTBjMzMwZzc3OGUTAKRsEIBQAekhF1l2tNXQAAAABJRU5ErkJggg== 1x">
                """;

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testLongDataUriInSrcsetCausesPathException() {
        String base64Data = "A".repeat(2000);
        String html = "<img srcset=\"data:image/png;base64," + base64Data + " 1x\">";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }

    @Test
    public void testDataUriInCssBackgroundImageUrl() {
        // Real case from projects-mobility.com: data URI in CSS background-image:url()
        String html = """
                <style>
                .aMqF6e .OQGVRy{background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAUoAAAAaCAYAAADR0BVGAAAACXBIWXMAAAsTAAALEwEAmpwYAAAKT2lDQ1BQaG90b3Nob3AgSUNDIHByb2ZpbGUAAHjanVNnVFPpFj333vRCS4iAlEtvUhUIIFJCi4AUkSYqIQkQSoghodkVUcERRUUEG8igiAOOjoCMFVEsDIoK2AfkIaKOg6OIisr74Xuja9a89+bN);background-repeat:no-repeat}
                </style>
                """;

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        // Data URIs in CSS should be skipped
        assertThat(links).isEmpty();
    }

    @Test
    public void testDataUriWithLeadingSpace() {
        // Data URI with leading space might bypass startsWith("data:") check
        String html = "<img src=\" data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAU\">";

        var links = JCrawler.extractLinksFromContent(1, html, null, "https://example.com/page.html")
                .toList();

        assertThat(links).isEmpty();
    }
}
