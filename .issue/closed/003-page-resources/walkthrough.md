# Walkthrough: Page Resources Implementation

## Changes
- Modified `HyperLink` to include `isResource`.
- Updated regex matchers to flag assets (images, scripts, css) as resources.
- Updated `CrawlerWorker` to always accept resource links.

## Verification
- Running `PageResourcesTest` passes.
    - Verified that internal navigation is accepted.
    - Verified that external navigation is rejected (and domain tracked).
    - Verified that external resources (images, scripts) are accepted (and domain NOT tracked as ignored).
    - Verified that links to files with extensions (.pdf, .zip, etc.) are treated as resources and accepted, even if pointing to valid external domains (or redirecting to them).
    - Verified that redirects from accepted internal pages to external pages are accepted (implying ownership).
    - Reduced logging verbosity:
        - `ParallelGraphTraverser` internal state logs moved to DEBUG.
        - Download/Cache status promoted to INFO for better visibility during standard runs.
    - Verified `localize` command works to export crawled content to a browsable folder.
- Command: `mvn test -Dtest=PageResourcesTest`
- Command (Localized): `jbang.cmd src/main/java/org/raisercostin/jcrawler/JCrawler.java --localize-inputs=.jcrawler --localize-output=.local-view`
    - NOTE: This command recursively localizes ALL content in `.jcrawler` into `.local-view`. Currently throws a `FileAlreadyExists` on some temporary backup files (`.bak`), but the main content is processed.
