package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Test for srcset URL extraction, particularly URLs with commas in query
 * parameters.
 * This tests the logic that parses srcset attributes.
 */
class SrcsetExtractionTest {

  @Test
  void testParseSrcsetWithCommas() {
    // Given: srcset string with URLs containing commas (like Wix uses)
    String srcset = "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_315,h_305,q_90,enc_avif,quality_auto/image.jpeg 1x, https://static.wixstatic.com/media/image.jpeg/v1/fit/w_630,h_610,q_90,enc_avif,quality_auto/image.jpeg 2x, https://static.wixstatic.com/media/image.jpeg/v1/fit/w_945,h_915,q_90,enc_avif,quality_auto/image.jpeg 3x, https://static.wixstatic.com/media/image.jpeg/v1/fit/w_1260,h_1220,q_90,enc_avif,quality_auto/image.jpeg 4x, https://static.wixstatic.com/media/image.jpeg/v1/fit/w_1501,h_1453,q_90,enc_avif,quality_auto/image.jpeg 5x";

    // When: Parse srcset using the same logic as JCrawler
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: All 5 entries should be extracted
    assertThat(entries)
        .as("Should extract all 5 srcset entries")
        .hasSize(5);

    // Verify specific URLs are extracted (with commas intact, no spaces)
    assertThat(entries.get(0).url)
        .as("Should extract 1x version")
        .isEqualTo(
            "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_315,h_305,q_90,enc_avif,quality_auto/image.jpeg");
    assertThat(entries.get(0).descriptor).isEqualTo("1x");

    assertThat(entries.get(1).url)
        .as("Should extract 2x version")
        .isEqualTo(
            "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_630,h_610,q_90,enc_avif,quality_auto/image.jpeg");
    assertThat(entries.get(1).descriptor).isEqualTo("2x");

    assertThat(entries.get(2).url)
        .as("Should extract 3x version (w_945,h_915)")
        .isEqualTo(
            "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_945,h_915,q_90,enc_avif,quality_auto/image.jpeg");
    assertThat(entries.get(2).descriptor).isEqualTo("3x");

    assertThat(entries.get(3).url)
        .as("Should extract 4x version")
        .isEqualTo(
            "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_1260,h_1220,q_90,enc_avif,quality_auto/image.jpeg");
    assertThat(entries.get(3).descriptor).isEqualTo("4x");

    assertThat(entries.get(4).url)
        .as("Should extract 5x version")
        .isEqualTo(
            "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_1501,h_1453,q_90,enc_avif,quality_auto/image.jpeg");
    assertThat(entries.get(4).descriptor).isEqualTo("5x");
  }

  @Test
  void testParseSrcsetWithHttpsAndRelative() {
    // Given: srcset with mixed protocols and separators
    String srcset = "/local/image.png 1x, https://external.com/image.png 2x, http://insecure.com/image.png 3x";

    // When: Parse
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: All extracted
    assertThat(entries).hasSize(3);
    assertThat(entries.get(0).url).isEqualTo("/local/image.png");
    assertThat(entries.get(1).url).isEqualTo("https://external.com/image.png");
    assertThat(entries.get(2).url).isEqualTo("http://insecure.com/image.png");
  }

  @Test
  void testParseSrcsetWithSpaces() {
    // Given: srcset where Jsoup has added spaces after commas within URLs
    String srcset = "https://static.wixstatic.com/media/image.jpeg/v1/fit/w_315, h_305, q_90/image.jpeg 1x, https://static.wixstatic.com/media/image.jpeg/v1/fit/w_630, h_610, q_90/image.jpeg 2x";

    // When: Parse srcset
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: URLs should have spaces removed
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).url)
        .as("Should extract URL with spaces removed")
        .isEqualTo("https://static.wixstatic.com/media/image.jpeg/v1/fit/w_315,h_305,q_90/image.jpeg");
    assertThat(entries.get(1).url)
        .as("Should extract URL with spaces removed")
        .isEqualTo("https://static.wixstatic.com/media/image.jpeg/v1/fit/w_630,h_610,q_90/image.jpeg");
  }

  @Test
  void testParseSrcsetWithRegexCommas() {
    // Given: srcset where URLs contain commas that aren't separators (typical in
    // image processing services)
    String srcset = "https://example.com/img,w_100,h_100.jpg 1x, https://example.com/img,w_200,h_200.jpg 2x";

    // When: Parse
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: Should separate correctly based on the lookahead pattern
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).url).isEqualTo("https://example.com/img,w_100,h_100.jpg");
    assertThat(entries.get(1).url).isEqualTo("https://example.com/img,w_200,h_200.jpg");
  }

  @Test
  void testParseSrcsetWithDecimalDescriptors() {
    // Given: srcset with decimal descriptors (e.g. 1.5x)
    String srcset = "image1.jpg 1x, image1.5.jpg 1.5x, image2.jpg 2x";

    // When: Parse
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: Should extract all 3
    assertThat(entries).hasSize(3);
    assertThat(entries.get(1).url).isEqualTo("image1.5.jpg");
    assertThat(entries.get(1).descriptor).isEqualTo("1.5x");
  }

  @Test
  void testParseSrcsetWithRelativeUrlsAndCommas() {
    // Given: Relative URLs that don't start with / or http
    String srcset = "img/w_100,h_100.jpg 100w, img/w_200,h_200.jpg 200w";

    // When: Parse
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: Should separate correctly
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).url).isEqualTo("img/w_100,h_100.jpg");
    assertThat(entries.get(1).url).isEqualTo("img/w_200,h_200.jpg");
  }

  @Test
  void testParseSrcsetFromUserReport() {
    // Given: The specific string reported by user
    String srcset = "static.wixstatic.com/media/c3db1c_2b7c791008c64d2b8bf459601e3b4220~mv2.png/v1/crop/x_42,y_0,w_557,h_557/fill/w_52,h_53,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/image.png 1x, https://static.wixstatic.com/media/c3db1c_2b7c791008c64d2b8bf459601e3b4220~mv2.png/v1/crop/x_42,y_0,w_557,h_557/fill/w_104,h_106,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/image.png 2x";

    // When: Parse
    List<SrcsetEntry> entries = parseSrcset(srcset);

    // Then: Should extract 2 entries
    assertThat(entries).hasSize(2);

    assertThat(entries.get(0).url).isEqualTo(
        "static.wixstatic.com/media/c3db1c_2b7c791008c64d2b8bf459601e3b4220~mv2.png/v1/crop/x_42,y_0,w_557,h_557/fill/w_52,h_53,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/image.png");
    assertThat(entries.get(0).descriptor).isEqualTo("1x");

    assertThat(entries.get(1).url).isEqualTo(
        "https://static.wixstatic.com/media/c3db1c_2b7c791008c64d2b8bf459601e3b4220~mv2.png/v1/crop/x_42,y_0,w_557,h_557/fill/w_104,h_106,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/image.png");
    assertThat(entries.get(1).descriptor).isEqualTo("2x");
  }

  /**
   * Parse srcset using the same logic as JCrawler.
   * This mirrors the implementation in JCrawler.CrawlerWorker.
   */
  private List<SrcsetEntry> parseSrcset(String srcset) {
    List<SrcsetEntry> entries = new ArrayList<>();

    // Heuristic Splitter:
    // 1. Split if comma is preceded by a descriptor (e.g., " 1x", " 1.5x", "
    // 200w").
    // Pattern: (?<=\s\d{1,5}(?:\.\d+)?[wx])\s*,\s+
    // 2. Split if comma is followed by an absolute URL or root path.
    // Pattern: ,\s+(?=(?:https?://|/))

    String splitRegex = "(?<=\\s\\d{1,5}(?:\\.\\d+)?[wx])\\s*,\\s+|" +
        ",\\s+(?=(?:https?://|/))";

    java.util.regex.Pattern splitPattern = java.util.regex.Pattern.compile(splitRegex);
    java.util.regex.Matcher splitMatcher = splitPattern.matcher(srcset);

    int lastEnd = 0;
    io.vavr.collection.List<String> srcsetEntries = io.vavr.collection.List.empty();

    while (splitMatcher.find()) {
      srcsetEntries = srcsetEntries.append(srcset.substring(lastEnd, splitMatcher.start()));
      lastEnd = splitMatcher.end();
    }
    srcsetEntries = srcsetEntries.append(srcset.substring(lastEnd));

    for (String entry : srcsetEntries) {
      if (!entry.trim().isEmpty()) {
        parseEntry(entry, entries);
      }
    }

    return entries;
  }

  private void parseEntry(String entry, List<SrcsetEntry> entries) {
    // Split by last space to separate URL from descriptor (e.g., "1x", "2x",
    // "100w")
    int lastSpace = entry.lastIndexOf(' ');
    if (lastSpace > 0) {
      String srcsetUrl = entry.substring(0, lastSpace).trim();
      String descriptor = entry.substring(lastSpace + 1).trim();

      // Remove spaces that Jsoup added within the URL (e.g., "w_263, h_189" ->
      // "w_263,h_189")
      srcsetUrl = srcsetUrl.replaceAll("\\s+", "");

      entries.add(new SrcsetEntry(srcsetUrl, descriptor));
    }
  }

  private static class SrcsetEntry {
    final String url;
    final String descriptor;

    SrcsetEntry(String url, String descriptor) {
      this.url = url;
      this.descriptor = descriptor;
    }
  }
}
