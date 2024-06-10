package org.raisercostin.jcrawler;

import java.net.URLEncoder;
import java.util.regex.Pattern;

import io.vavr.collection.Iterator;
import lombok.SneakyThrows;
import lombok.val;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JSeeder {
  @SneakyThrows
  Iterator<String> findShopsWithJsoup(String query, int count) {
    val google = "http://www.google.com/search?q=";
    val search = query;
    val charset = "UTF-8";
    val userAgent = "ExampleBot 1.0 (+http://example.com/bot)";
    val url = google + URLEncoder.encode(search, charset) + "&num=" + count + "&start=0";
    System.out.println("search using " + url);
    val doc = Jsoup.connect(url).userAgent(userAgent).get();
    return extractLinks(doc);
  }

  private static final Pattern p = Pattern.compile("\\/url\\?q=(https?\\:\\/\\/.+)&sa");

  Iterator<String> extractLinks(Document doc) {
    val select = Iterator.of(doc.select("a"))
      .map(x -> x.attr("href"))
      .map(href -> Iterator.continually(p.matcher(href))
        .takeWhile(matcher -> matcher.find())
        .headOption()
        .map(x -> x.group(1)));

    val a = select.flatMap(x -> x);
    //val b = a.zipWithIndex();
    //.map(x -> x.swap()).toMap().toSeq.map(_.swap).sortBy(_._2);
    //    log.info(
    //      "Request-Level API: received {} response with {} bytes",
    //      response.status, Jsoup.parse(content))
    //b.map(x=>SiteDetails(x._1,x._2, None));
    return a;
  }
}
