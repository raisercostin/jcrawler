# Issue: Remaining Crawl Validation and Access Errors

## Description
A full crawl of `projects-mobility.com` reveals several classes of errors that persist despite recent fixes for data URIs and template variables.

## Symptoms
1. **Template Variable Bypass**:
   - URLs containing `$%7B...%7D` (encoded `${...}`) are not currently caught by the filter and cause `IllegalArgumentException` during URI creation/download.
   - Example: `https://www.projects-mobility.com/blog/categories/$%7Bi.uri%7D`

2. **Long URI Validation Failures**:
   - Extremely long URLs (Wix/Thunderbolt dynamic model APIs) with massive query strings are failing with `IllegalArgumentException`.
   - Example found in logs: `https://www.projects-mobility.com/_api/v2/dynamic-model?additionalComponents=true&...`

3. **File Access Issues**:
   - `java.nio.file.AccessDeniedException` encountered on temporary files (`.tmp2.gz`) in the `.jcrawler` cache directory.
   - Example: `D:\home\raiser\work\namek-jcrawl\.jcrawler\siteassets.parastorage.com\pages\pages\thunderbolt.tmp2.gz`

## Proposed Fixes
- **Better Template Filtering**: Expand the detection in `extractLinksFromContent` to include encoded variants like `$%7B` and `$%7D`.
- **Robust URI Handling**: Use a more lenient URI parser or explicitly skip URLs that exceed a reasonable length (e.g., 2048 characters) if they are known dynamic APIs.
- **Concurrent Access Handling**: Investigate if multiple threads are trying to write/rename the same temporary file simultaneously, leading to `AccessDeniedException`.

## Verification
- Run `jbang.cmd src/main/java/org/raisercostin/jcrawler/JCrawler.java https://www.projects-mobility.com/` and confirm these specific errors are resolved or gracefully handled.
