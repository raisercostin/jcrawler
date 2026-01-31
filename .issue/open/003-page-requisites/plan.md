# Page Resources vs External Navigation Plan

## Goal
To enhance `jcrawler` to behave more like standard tools by distinguishing between **Page Resources** (assets implicitly loaded or consumed: images, CSS, JS, media) and **Navigation Links** (anchors to other pages requires user action).

## Proposed Changes

### 1. Update `HyperLink`
Add a field to distinguish the link type.
```java
public class HyperLink {
  // ...
  boolean isResource; // true if it's an asset/resource, false if navigation
  // ...
  public static HyperLink of(..., boolean isResource) { ... }
}
```

### 2. Update `LinkMatcher` in `JCrawler`
Update the inner class `LinkMatcher` to include the type.
```java
public static class LinkMatcher {
  // ...
  public final boolean isResource;
}
```

### 3. Update Regex Definitions in `JCrawler`
- `exp`: `<a href>` -> `isResource = false` (Navigation)
- `imgExp`, `scriptSrc`, `linkTagExp`, `urlInStyleExp` -> `isResource = true`
- `robotsTxtExp`, `sitemapLocExp` -> `isResource = true`

### 4. Update Extraction Logic
Update `extractLinksFromContent` to populate `HyperLink.isResource` based on the `LinkMatcher` that found the link.
**Refinement**: Also check file extensions for `<a>` tags. If a link points to a known resource type (pdf, zip, mp3, etc.), mark `isResource = true`.

### 5. Update `CrawlerWorker.accept()`
Modify the filter logic:
```java
if (link.isResource) {
    return true; // Always accept resources
}
// Proceed with existing domain/accept logic for navigation links
```

## Verification Plan

### Automated Test
Create `PageResourcesTest.java`:
1.  Mock a page `index.html` on `mysite.com`.
2.  Embed `<img src="https://cdn.external.com/logo.png">`.
3.  Link to `<a href="https://other.com/page">`.
4.  Crawl `mysite.com`.
5.  Assert:
    -   `cdn.external.com` content is downloaded.
    -   `other.com` is rejected.

### Manual Verification
Run against a real site with external assets.
