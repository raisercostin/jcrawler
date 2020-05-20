package org.raisercostin.jcrawl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.url.HttpClientLocation;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@Slf4j
public class HyperLink {
  public static HyperLink of(String relativeOrAbsoluteHyperlink, String text, String all, String sourceHyperlink,
      String sourceLocal) {
    HttpClientLocation link = Locations.url(sourceHyperlink, relativeOrAbsoluteHyperlink);
    //TODO link should not contain #fragments since link is used for uniqueness
    return new HyperLink(link.toExternalForm(), relativeOrAbsoluteHyperlink, text, all, sourceHyperlink, sourceLocal);
  }

  String link;
  String relativeOrAbsoluteHyperlink;
  @ToString.Exclude
  String text;
  @ToString.Exclude
  String all;
  String sourceHyperlink;
  String sourceLocal;

  @ToString.Include
  String text() {
    return StringUtils.abbreviate(text, 100).replaceAll("\\s+", " ");
  }

  public HttpClientLocation link() {
    return Locations.url(link);
  }
}
