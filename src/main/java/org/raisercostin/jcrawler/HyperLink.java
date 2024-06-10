package org.raisercostin.jcrawler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.url.SimpleUrl;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HyperLink {
  public static HyperLink of(String url) {
    return new HyperLink(url, "original", "", null, null, null, null);
  }

  public static HyperLink of(String relativeOrAbsoluteHyperlink, String text, String anchor, String all,
      String sourceHyperlink,
      String localCache) {
    String url = SimpleUrl.resolve(sourceHyperlink, relativeOrAbsoluteHyperlink);
    //WebClientLocation link = Locations.url(sourceHyperlink, relativeOrAbsoluteHyperlink);
    //TODO link should not contain #fragments since link is used for uniqueness
    return new HyperLink(url, relativeOrAbsoluteHyperlink, text, anchor, all, sourceHyperlink, localCache);
  }

  @EqualsAndHashCode.Include
  String externalForm;
  String relativeOrAbsoluteHyperlink;
  @ToString.Exclude
  String text;
  String anchor;
  @ToString.Exclude
  String all;
  String sourceHyperlink;
  String localCache;

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
}
