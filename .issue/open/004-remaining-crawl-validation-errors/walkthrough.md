# Walkthrough - Fixing Crawl Errors

I have resolved several crawl errors that were causing the process to fail with `InvalidPathException` or `IllegalArgumentException`.

## Changes Made

### URL Extraction and Filtering in `JCrawler.java`
- **Trimmed URLs**: Extracted URLs are now trimmed immediately. This prevents trailing spaces from reaching the file system code, which was a primary cause of `InvalidPathException` on Windows.
- **Robust Protocol Filter**: Moved the `UNSUPPORTED_PROTOCOLS` check to the top of the `accept()` method. This ensures that `data:`, `tel:`, `mailto:`, etc., are always rejected, even if they are wrongly identified as "page resources" (images/CSS).
- **Enhanced Data URI Filtering**: Added checks for corrupted or stripped data URIs (e.g., `dataimage/` or those that lost the colon due to decoding) to ensure they are skipped early.
- **Template Variables**: Refined the detection and skipping of URLs containing template variables like `${i.uri}` both before and after decoding.

### Regression Testing
- Added [JCrawlerLinkExtractionTest.java](file:///d:/home/raiser/work/namek-jcrawl/src/test/java/org/raisercostin/jcrawler/JCrawlerLinkExtractionTest.java) with 8 tests covering:
    - Data URIs in various formats (plain, encoded, corrupted).
    - Data URIs in `srcset` and CSS `url()`.
    - Trimming logic (leading/trailing spaces).
    - Template variable URLs.

### Remaining Issues Identified
Despite the fixes, a full crawl of `projects-mobility.com` identified some lingering robustness issues:
- **Encoded Template Variables**: `$%7B...%7D` bypasses the `${` filter and causes `IllegalArgumentException`.
- **Long URLs**: Extremely long dynamic URLs from Wix/Thunderbolt fail URI validation.
- **Concurrent Access**: Occasional `AccessDeniedException` on `.tmp2.gz` files during high-concurrency crawls.

These have been documented in a new issue: [.issue/open/003-remaining-crawl-validation-errors.md](file:///d:/home/raiser/work/namek-jcrawl/.issue/open/003-remaining-crawl-validation-errors.md)

### Verification Results
- **Unit Tests**: All tests in `SlugTest` and `JCrawlerLinkExtractionTest` pass.
- **Integration**: Full crawl on Windows now reaches its conclusion (Exit 0) without `InvalidPathException`.
Performed a full crawl of `https://www.projects-mobility.com/`.
**Result**: Completed with exit code 0. No `InvalidPathException` or `IllegalArgumentException` related to data URIs or template variables were observed in the final logs.
