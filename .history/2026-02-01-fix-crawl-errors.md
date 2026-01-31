# History: Fixing Crawl Errors (Data URIs & Template Variables)

**Date**: 2026-02-01
**Topic**: data-uri-invalid-path-exception

## Context
The crawler was failing on `www.projects-mobility.com` with `java.nio.file.InvalidPathException: Trailing char < >` and `IllegalArgumentException`.

## Findings
- **Data URIs in CSS**: Found that `url(data:...)` in style tags was being extracted but not always filtered correctly.
- **Corrupted Data URIs**: Decoding double-encoded URLs sometimes resulted in corrupted URIs (`dataimage/` without colon) or URIs with trailing spaces.
- **Windows Path Limitations**: Trailing spaces in data URIs caused `InvalidPathException` during path creation in `Slug.java`.
- **Accept Filter Bypass**: The `accept2` method was allowing all resources (images/CSS) to bypass protocol checks (`UNSUPPORTED_PROTOCOLS`), leading to data URIs being processed.

## Solution
1. **Trimming**: Implemented mandatory `trim()` on all extracted URLs in `extractLinksFromContent`.
2. **Robust Filtering**: Moved `UNSUPPORTED_PROTOCOLS` check to the top of `accept()` to ensure `data:` is always rejected even if marked as a resource.
3. **Template Variables**: Added logic to skip URLs containing `${...}` both before and after decoding.

## Verification
- Added `JCrawlerLinkExtractionTest.java` with 8 cases.
- Consolidated `SlugTest.java` to document sanitization behavior.
- Verified with full crawl: Exit code 0, no `InvalidPathException`.
