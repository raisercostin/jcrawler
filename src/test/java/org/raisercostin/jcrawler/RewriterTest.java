package org.raisercostin.jcrawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.Metadata;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.nodes.Nodes;

class RewriterTest {

  @TempDir
  Path tempDir;

  @Test
  void testSrcsetLocalizationWithJsoup() throws Exception {
    // 1. Setup: Create a source directory with HTML containing srcset
    PathLocation sourceDir = Locations.path(tempDir.resolve("source"));
    sourceDir.mkdirIfNeeded();

    // Create HTML with srcset (URLs with commas in query params, like Wix uses)
    String htmlContent = """
        <!DOCTYPE html>
        <html>
        <body>
            <picture>
                <source srcset="https://static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90,enc_avif,quality_auto/image.png 1x, https://static.wixstatic.com/media/image.png/v1/fill/w_526,h_378,q_90,enc_avif,quality_auto/image.png 2x" type="image/png">
                <img src="https://static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90,enc_avif,quality_auto/image.png" alt="Test">
            </picture>
        </body>
        </html>
        """;

    PathLocation htmlFile = sourceDir.child("test.html");
    htmlFile.write(htmlContent);

    // Create metadata for the HTML file
    Metadata htmlMeta = new Metadata();
    htmlMeta.url = "https://example.com/test.html";
    htmlFile.meta("", ".meta.json").write(Nodes.json.toString(htmlMeta));

    // Create the downloaded images with metadata
    PathLocation img1x = sourceDir
        .child("static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90,enc_avif,quality_auto/image.png");
    img1x.parent().get().mkdirIfNeeded();
    img1x.write("fake image 1x");
    Metadata meta1x = new Metadata();
    meta1x.url = "https://static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90,enc_avif,quality_auto/image.png";
    img1x.meta("", ".meta.json").write(Nodes.json.toString(meta1x));

    PathLocation img2x = sourceDir
        .child("static.wixstatic.com/media/image.png/v1/fill/w_526,h_378,q_90,enc_avif,quality_auto/image.png");
    img2x.parent().get().mkdirIfNeeded();
    img2x.write("fake image 2x");
    Metadata meta2x = new Metadata();
    meta2x.url = "https://static.wixstatic.com/media/image.png/v1/fill/w_526,h_378,q_90,enc_avif,quality_auto/image.png";
    img2x.meta("", ".meta.json").write(Nodes.json.toString(meta2x));

    // 2. Run localization
    PathLocation outputDir = Locations.path(tempDir.resolve("output"));
    Rewriter rewriter = new Rewriter();
    rewriter.run(java.util.List.of(sourceDir), outputDir);

    // 3. Verify: Parse the output HTML with Jsoup and check URLs
    PathLocation outputHtml = outputDir.child("test.html");
    assertThat(outputHtml.exists()).isTrue();

    String localizedHtml = outputHtml.readContent();
    Document doc = Jsoup.parse(localizedHtml);

    // Check img src
    Element img = doc.selectFirst("img");
    assertThat(img).isNotNull();
    String imgSrc = img.attr("src");
    assertThat(imgSrc)
        .as("img src should be localized to relative path (no https://)")
        .doesNotContain("https://")
        .contains("static.wixstatic.com/media/"); // Domain is part of the relative path structure

    // Check source srcset
    Element source = doc.selectFirst("source");
    assertThat(source).isNotNull();
    String srcset = source.attr("srcset");

    // Verify srcset contains localized paths (not external URLs with https://)
    assertThat(srcset)
        .as("srcset should not contain https:// (should be relative paths)")
        .doesNotContain("https://");

    // Verify both 1x and 2x versions are localized
    assertThat(srcset)
        .as("srcset should contain localized 1x image")
        .contains("static.wixstatic.com/media/image.png/v1/fill/w_263");

    assertThat(srcset)
        .as("srcset should contain localized 2x image")
        .contains("static.wixstatic.com/media/image.png/v1/fill/w_526");

    // Verify descriptors are preserved
    assertThat(srcset)
        .as("srcset should preserve 1x descriptor")
        .contains("1x");

    assertThat(srcset)
        .as("srcset should preserve 2x descriptor")
        .contains("2x");
  }

  @Test
  void testUrlNormalizationWithSpaces() throws Exception {
    // Test that URLs with spaces (added by Jsoup) are properly normalized
    PathLocation sourceDir = Locations.path(tempDir.resolve("source2"));
    sourceDir.mkdirIfNeeded();

    // Simulate what Jsoup does: adds spaces after commas
    String htmlWithSpaces = """
        <!DOCTYPE html>
        <html>
        <body>
            <img src="https://static.wixstatic.com/media/image.png/v1/fill/w_263, h_189, q_90/image.png" alt="Test">
        </body>
        </html>
        """;

    PathLocation htmlFile = sourceDir.child("test2.html");
    htmlFile.write(htmlWithSpaces);

    Metadata htmlMeta = new Metadata();
    htmlMeta.url = "https://example.com/test2.html";
    htmlFile.meta("", ".meta.json").write(Nodes.json.toString(htmlMeta));

    // Create image with metadata (URL WITHOUT spaces, as it would be downloaded)
    PathLocation img = sourceDir.child("static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90/image.png");
    img.parent().get().mkdirIfNeeded();
    img.write("fake image");
    Metadata imgMeta = new Metadata();
    imgMeta.url = "https://static.wixstatic.com/media/image.png/v1/fill/w_263,h_189,q_90/image.png";
    img.meta("", ".meta.json").write(Nodes.json.toString(imgMeta));

    // Run localization
    PathLocation outputDir = Locations.path(tempDir.resolve("output2"));
    Rewriter rewriter = new Rewriter();
    rewriter.run(java.util.List.of(sourceDir), outputDir);

    // Verify
    PathLocation outputHtml = outputDir.child("test2.html");
    String localizedHtml = outputHtml.readContent();
    Document doc = Jsoup.parse(localizedHtml);

    Element imgElement = doc.selectFirst("img");
    String imgSrc = imgElement.attr("src");

    assertThat(imgSrc)
        .as("img src should be localized despite spaces in source HTML")
        .doesNotContain("https://")
        .startsWith("static.wixstatic.com/media/");
  }

  @Test
  void testInlineAssetLocalization() throws Exception {
    PathLocation sourceDir = Locations.path(tempDir.resolve("source-inline"));
    sourceDir.mkdirIfNeeded();

    String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            .bg { background-image: url("https://static.wixstatic.com/media/bg.jpg"); }
          </style>
        </head>
        <body>
            <div style="background: url('https://static.wixstatic.com/media/icon.png');"></div>
            <script>
              var config = { "logo": "https://static.wixstatic.com/media/logo.png" };
            </script>
        </body>
        </html>
        """;

    PathLocation htmlFile = sourceDir.child("test.html");
    htmlFile.write(htmlContent);

    Metadata htmlMeta = new Metadata();
    htmlMeta.url = "https://example.com/test.html";
    htmlFile.meta("", ".meta.json").write(Nodes.json.toString(htmlMeta));

    // Register resources in globalUrlMap via existing files
    createResource(sourceDir, "https://static.wixstatic.com/media/bg.jpg", "static.wixstatic.com/media/bg.jpg");
    createResource(sourceDir, "https://static.wixstatic.com/media/icon.png", "static.wixstatic.com/media/icon.png");
    createResource(sourceDir, "https://static.wixstatic.com/media/logo.png", "static.wixstatic.com/media/logo.png");

    // Run localization
    PathLocation outputDir = Locations.path(tempDir.resolve("output-inline"));
    Rewriter rewriter = new Rewriter();
    rewriter.run(java.util.List.of(sourceDir), outputDir);

    // Verify
    String localizedHtml = outputDir.child("test.html").readContent();

    assertThat(localizedHtml)
        .as("Inline style should be localized")
        .contains("url(\"static.wixstatic.com/media/bg.jpg\")")
        .doesNotContain("https://static.wixstatic.com/media/bg.jpg");

    assertThat(localizedHtml)
        .as("Style attribute should be localized")
        .contains("url('static.wixstatic.com/media/icon.png')")
        .doesNotContain("https://static.wixstatic.com/media/icon.png");

    assertThat(localizedHtml)
        .as("Inline script should be localized")
        .contains("\"logo\": \"static.wixstatic.com/media/logo.png\"")
        .doesNotContain("https://static.wixstatic.com/media/logo.png");
  }

  private void createResource(PathLocation dir, String url, String path) {
    PathLocation file = dir.child(path);
    file.parent().get().mkdirIfNeeded();
    file.write("fake content");
    Metadata meta = new Metadata();
    meta.url = url;
    file.meta("", ".meta.json").write(Nodes.json.toString(meta));
  }
}
