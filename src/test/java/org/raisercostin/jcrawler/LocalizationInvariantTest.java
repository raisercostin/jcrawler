package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.Metadata;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.nodes.Nodes;

/**
 * Property-based test that verifies the localization invariant:
 * "After crawling and localizing, there should be no external https:// URLs to
 * resources that were downloaded"
 */
class LocalizationInvariantTest {

  @TempDir
  Path tempDir;

  @Test
  void testLocalizationInvariant_NoExternalUrlsForDownloadedResources() throws Exception {
    // 1. Setup: Use existing .jcrawler data (must exist from previous crawl)
    PathLocation crawlDir = Locations.path(".jcrawler");
    PathLocation localViewDir = Locations.path(tempDir.resolve("local-view-test"));

    // Verify .jcrawler exists
    assertThat(crawlDir.exists())
        .as(".jcrawler directory must exist (run a crawl first)")
        .isTrue();

    // 2. Run localization on existing crawl data
    runJCrawler(
        "--localize-inputs=" + crawlDir.toPath().toAbsolutePath().toString(),
        "--localize-output=" + localViewDir.toPath().toAbsolutePath().toString());

    // Verify localization created files
    assertThat(localViewDir.exists())
        .as("Localized output directory should exist")
        .isTrue();

    // 3. Verify the invariant: No external Wix static.com URLs
    verifyNoWixStaticUrls(localViewDir);
  }

  /**
   * Verify that no https://static.wixstatic.com URLs remain in localized HTML.
   */
  private void verifyNoWixStaticUrls(PathLocation localViewDir) throws Exception {
    // Find all HTML files in the localized output
    List<Path> htmlFiles = Files.walk(localViewDir.toPath())
        .filter(p -> p.toString().endsWith(".html"))
        .collect(Collectors.toList());

    assertThat(htmlFiles)
        .as("Should have at least one HTML file in localized output")
        .isNotEmpty();

    System.out.println("Checking " + htmlFiles.size() + " HTML files for external Wix URLs...");

    // Check each HTML file for Wix static URLs
    for (Path htmlFile : htmlFiles) {
      String content = Files.readString(htmlFile);

      // Check for any https://static.wixstatic.com URLs
      if (content.contains("https://static.wixstatic.com")) {
        // Find and report the specific URLs
        Pattern wixPattern = Pattern.compile("https://static\\.wixstatic\\.com[^\\s\"'<>]+");
        Matcher matcher = wixPattern.matcher(content);

        List<String> foundUrls = new ArrayList<>();
        while (matcher.find() && foundUrls.size() < 5) {
          foundUrls.add(matcher.group());
        }

        assertThat(false)
            .as("Found external Wix URLs in " + htmlFile + ":\n" +
                String.join("\n", foundUrls) +
                "\n\nAll Wix resources should be localized to relative paths!")
            .isTrue();
      }
    }

    System.out.println("✅ No external Wix URLs found - all resources properly localized!");
  }

  /**
   * Helper method to run JCrawler.main() with the given arguments.
   * Captures and prints output for debugging.
   */
  private void runJCrawler(String... args) {
    System.out.println("Running JCrawler with args: " + String.join(" ", args));

    // Capture System.out/err
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    java.io.PrintStream originalOut = System.out;
    java.io.PrintStream originalErr = System.err;

    try {
      System.setOut(new java.io.PrintStream(out));
      System.setErr(new java.io.PrintStream(out));

      JCrawler.main(args, false);

    } catch (Throwable e) {
      String output = out.toString();
      if (!output.isEmpty()) {
        System.err.println("JCrawler output before failure:\n" + output);
      }
      if (e instanceof RuntimeException && e.getMessage().contains("exit code: 1")) {
        // This is the RichCli exit, show output and fail
        assertThat(false).as("JCrawler failed with exit code 1. Output:\n" + output).isTrue();
      }
      throw e;
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    String output = out.toString();
    if (!output.isEmpty()) {
      System.out.println("JCrawler output:\n" + output);
    }
  }

}
