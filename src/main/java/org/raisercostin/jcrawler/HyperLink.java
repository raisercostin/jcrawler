package org.raisercostin.jcrawler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.url.SimpleUrl;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HyperLink {
  public static HyperLink of(String url) {
    return new HyperLink(url, 0, toLocalCache(url), "original", "", null, null, null, null, false, false);
  }

  public static HyperLink of(String relativeOrAbsoluteHyperlink, int depth, String text,
      String anchor, String all, String sourceHyperlink, String sourceLocalCache) {
    return of(relativeOrAbsoluteHyperlink, depth, text, anchor, all, sourceHyperlink, sourceLocalCache, false, false);
  }

  public static HyperLink of(String relativeOrAbsoluteHyperlink, int depth, String text,
      String anchor, String all, String sourceHyperlink, String sourceLocalCache, boolean isRedirect) {
    return of(relativeOrAbsoluteHyperlink, depth, text, anchor, all, sourceHyperlink, sourceLocalCache, isRedirect,
        false);
  }

  public static HyperLink of(String relativeOrAbsoluteHyperlink, int depth, String text,
      String anchor, String all, String sourceHyperlink, String sourceLocalCache, boolean isRedirect,
      boolean isResource) {
    String url = SimpleUrl.resolve(sourceHyperlink, relativeOrAbsoluteHyperlink);
    // WebClientLocation link = Locations.url(sourceHyperlink,
    // relativeOrAbsoluteHyperlink);
    // TODO link should not contain #fragments since link is used for uniqueness
    return new HyperLink(url, depth, toLocalCache(url), relativeOrAbsoluteHyperlink, text, anchor, all, sourceHyperlink,
        sourceLocalCache, isRedirect, isResource);
  }

  private static Slug toLocalCache(String url) {
    return Slug.slugs(url).head();
  }

  @EqualsAndHashCode.Include
  String externalForm;
  int depth;
  Slug localCache;
  String relativeOrAbsoluteHyperlink;
  @ToString.Exclude
  String text;
  String anchor;
  @ToString.Exclude
  String all;
  String sourceHyperlink;
  String sourceLocalCache;
  boolean isRedirect;
  boolean isResource;

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
