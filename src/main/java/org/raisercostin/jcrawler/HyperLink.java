package org.raisercostin.jcrawler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jcrawler.SlugEscape.Slug;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.url.SimpleUrl;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HyperLink {
  public static HyperLink of(String url) {
    return new HyperLink(url, toLocalCache(url), "original", "", null, null, null, null);
  }

  public static HyperLink of(String relativeOrAbsoluteHyperlink, String text,
      String anchor, String all, String sourceHyperlink, String sourceLocalCache) {
    String url = SimpleUrl.resolve(sourceHyperlink, relativeOrAbsoluteHyperlink);
    //WebClientLocation link = Locations.url(sourceHyperlink, relativeOrAbsoluteHyperlink);
    //TODO link should not contain #fragments since link is used for uniqueness
    return new HyperLink(url, toLocalCache(url), relativeOrAbsoluteHyperlink, text, anchor, all, sourceHyperlink,
      sourceLocalCache);
  }

  private static Slug toLocalCache(String url) {
    return SlugEscape.toSlug(url);
  }

  @EqualsAndHashCode.Include
  String externalForm;
  Slug localCache;
  String relativeOrAbsoluteHyperlink;
  @ToString.Exclude
  String text;
  String anchor;
  @ToString.Exclude
  String all;
  String sourceHyperlink;
  String sourceLocalCache;

  @ToString.Include
  String text() {
    return StringUtils.abbreviate(text, 100).replaceAll("\\s+", " ");
  }

  public SimpleUrl link(boolean keepQuery) {
    return SimpleUrl.from(externalForm, keepQuery);
  }

  public String withoutQuery() {
    return SimpleUrl.from(externalForm).withoutQuery().toExternalForm();
  }

  @SneakyThrows
  public String hostname() {
    return SimpleUrl.from(externalForm).uri.getHost();
  }

  public Slug slug() {
    return toLocalCache(externalForm);
  }

  public String hostnameForAccept() {
    return StringUtils.removeStart(hostname(), "wwww.");
  }
}
