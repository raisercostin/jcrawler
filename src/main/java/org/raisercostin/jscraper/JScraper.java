package org.raisercostin.jscraper;

import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import models.Listing;
import org.raisercostin.jedi.Locations;
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.namek.app.NamekConsole;
import org.raisercostin.nodes.Nodes;
import scala.collection.Seq;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class JScraper {
  public static void scrape(PathLocation crawledDir) {
    crawledDir.parent().get().findFilesAndDirs(false).doOnNext(x -> scrapePage(x)).blockLast();
    //scrapePage(crawledDir);
  }

  @SneakyThrows
  private static void scrapePage(PathLocation page) {
    MetaInfo meta = page.readMeta();
    if (meta.httpResponseHeaderContentTypeIsHtml()) {
      Future<Seq<Listing>> res = new NamekConsole(null).extractPage(Locations.file(page.absoluteAndNormalized()));
      Seq<Listing> result = Await.result(res, scala.concurrent.duration.Duration.apply(10, TimeUnit.SECONDS));
      System.out.println(Nodes.json.toString(result));
    }
  }
}
