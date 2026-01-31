# Implementation Plan - Resolve Remaining Crawl Errors (Issue 004)

Identify and fix remaining crawl errors: template variable bypasses, long URL validation failures, and concurrent file access issues.

## Proposed Changes

### [Crawler Core]

#### [MODIFY] [JCrawler.java](file:///d:/home/raiser/work/namek-jcrawl/src/main/java/org/raisercostin/jcrawler/JCrawler.java)
- In `extractLinksFromContent`, add detection for `$%7B` and `$%7D` to skip encoded template variables.
- In `downloadAndExtractLinks`, wrap `download` call with more specific error handling for long URLs if needed, but primarily rely on better filtering.
- Ensure `destInitial` is more unique if possible, or synchronize access to it.

#### [MODIFY] [Slug.java](file:///d:/home/raiser/work/namek-jcrawl/src/main/java/org/raisercostin/jcrawler/Slug.java)
- Fix `contentPathInitial` to avoid collisions for URLs that only differ in query strings when `stripQueryParams` is true.
- Suggestion: Append a short hash of the full URL (including query) even to the "initial" path to ensure thread safety during concurrent downloads of similar URLs.

### [Testing]

#### [NEW] [UrlExtractionEdgeCasesTest.java](file:///d:/home/raiser/work/namek-jcrawl/src/test/java/org/raisercostin/jcrawler/UrlExtractionEdgeCasesTest.java)
- Add test cases for `$%7B...%7D` encoded template variables.
- Add test cases for extremely long URLs to see where they fail.
- Add a concurrency test to reproduce `AccessDeniedException` (if feasible in a unit test).

## Verification Plan

### Automated Tests
- `mvn test -Dtest=UrlExtractionEdgeCasesTest,JCrawlerLinkExtractionTest`

### Manual Verification
- Run a full crawl of `projects-mobility.com` using `jbang`:
  ```bash
  jbang.cmd src/main/java/org/raisercostin/jcrawler/JCrawler.java https://www.projects-mobility.com/
  ```
- Verify `crawl-output.log` for absence of `IllegalArgumentException` and `AccessDeniedException`.
