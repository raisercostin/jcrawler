# Task: Fix Crawl Errors

- [x] **Data URI Path Issues**
  - [x] Create `SlugTest.java` to replicate `InvalidPathException` <!-- id: 10 -->
  - [x] Fix `Slug` or `JCrawler` to handle data URIs safely during path creation <!-- id: 11 -->
- [x] **Better Template Filtering**
  - [x] Implement detection for encoded template variables (`$%7B` and `$%7D`) in `extractLinksFromContent`.
  - [x] Add regression tests in `JCrawlerTest`.
- [x] **Robust URI Handling**
  - [x] Investigate `IllegalArgumentException` for long Wix/Thunderbolt URLs.
  - [x] Implement a length limit or specific skip rule for known dynamic dynamic-model APIs.
- [x] **Concurrent Access Fix**
  - [x] Analyze `AccessDeniedException` on `.tmp2.gz` files.
  - [x] Ensure thread-safe temporary file creation and renaming.
- [x] **Identifying Remaining Errors**
  - [x] Run full crawl on projects-mobility.com <!-- id: 16 -->
  - [x] Analyze crawl output for exceptions and invalid URLs <!-- id: 17 -->
  - [x] Create a new issue in `.issue/open/` for remaining problems <!-- id: 18 -->
