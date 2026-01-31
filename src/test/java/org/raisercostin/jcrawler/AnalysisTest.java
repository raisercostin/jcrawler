package org.raisercostin.jcrawler;

//DEPS net.jqwik:jqwik:1.9.0
//DEPS commons-io:commons-io:2.16.1
//DEPS org.jsoup:jsoup:1.17.2
//SOURCES ../../../../../test/java/com/namekis/utils/RichTestCli.java

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.namekis.utils.RichTestCli;

public class AnalysisTest {

    @Property
    void allFilesShouldHaveNoHttpOrHttps(@ForAll("htmlFiles") Path path) throws IOException {
        String content = Files.readString(path);
        Document doc = Jsoup.parse(content);

        List<String> failures = new ArrayList<>();

        // Check all elements with href or src attributes
        Elements elements = doc.select("[href], [src]");

        List<String> literalExceptions = List.of(
                "http://www.w3.org",
                "https://www.w3.org");

        for (Element el : elements) {
            String tagName = el.tagName();
            String url = el.hasAttr("href") ? el.attr("href") : el.attr("src");
            String attrName = el.hasAttr("href") ? "href" : "src";

            if (url.startsWith("http://") || url.startsWith("https://")) {
                // Check exceptions
                boolean isException = literalExceptions.stream().anyMatch(ex -> url.startsWith(ex));
                if (!isException) {
                    // Context-aware reporting
                    String type = "unknown";
                    if (tagName.equals("a") || tagName.equals("link")) {
                        type = "link";
                    } else if (tagName.equals("img") || tagName.equals("script") || tagName.equals("iframe")) {
                        type = "resource";
                    }

                    failures.add(
                            String.format("[%s] Found external usage in <%s %s=\"%s\">", type, tagName, attrName, url));
                }
            }
        }

        // Also check for inline styles with url()
        Elements styles = doc.select("[style]");
        for (Element el : styles) {
            String style = el.attr("style");
            if (style.contains("http://") || style.contains("https://")) {
                // Simple check, detailed regex could be better but this is a starter
                failures.add(String.format("[style] Found external usage in style: \"%s\"", style));
            }
        }

        if (!failures.isEmpty()) {
            System.err.println("----------------------------------------------------------------");
            System.err.println("FAILURE in file: " + path);
            System.err.println("Found " + failures.size() + " forbidden URLs (showing first 10):");
            failures.stream().limit(10).forEach(msg -> System.err.println(" - " + msg));
            System.err.println("----------------------------------------------------------------");
        }

        Assertions.assertThat(failures)
                .as("File %s should not contain http/https (exceptions excluded)", path)
                .isEmpty();
    }

    @Provide
    public Arbitrary<Path> htmlFiles() throws IOException {
        Path root = Paths.get(".local-view-test");
        List<Path> files = new ArrayList<>();
        if (Files.exists(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(p -> p.toString().endsWith(".html"))
                        .collect(Collectors.toList());
            }
        }
        if (files.isEmpty()) {
            return Arbitraries.of(files);
        }
        return Arbitraries.of(files);
    }

    public static void main(String... args) {
        RichTestCli.main2(args, "org.raisercostin.jcrawler.AnalysisTest");
    }
}
