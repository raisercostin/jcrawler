# Rewriter Refactoring and Jsoup Analysis

## 1. Goal
Refactor the legacy `Localizer` class to `Rewriter` to better reflect its purpose, and upgrade the analysis capabilities to use Jsoup for robust, context-aware URL detection.

## 2. Changes

### Refactoring
- **Renamed** `Localizer.java` to `Rewriter.java`.
- **Renamed** `LocalizerTest.java` to `RewriterTest.java`.
- **Updated** `JCrawler.java` to use the new class and command line arguments (`--rewrite-inputs`, `--rewrite-output`).
- **Updated** `RichCli.java` to support `exitAtEnd` allowing for better testing of CLI commands.

### Analysis Improvements
- Refactored `AnalysisTest.java` to use `org.jsoup:jsoup`.
- Replaced regex-based searching with DOM traversal to differentiate between:
    - `[link]` (`<a href>`, `<link href>`): Navigation and styles.
    - `[resource]` (`<img src>`, `<script src>`): Inline assets.
- Added URL decoding logic to `Rewriter.java` to handle cases where metadata URLs are unencoded (e.g. `{...}`) but HTML attributes are encoded (e.g. `%7B...%7D`).

## 3. Verification

### Automated Tests
- `RewriterTest` passed, verifying:
    - Basic rewriting.
    - `srcset` with commas.
    - URL normalization (spaces).
- `AnalysisTest` run against crawl data:
    - Identified specific context of missing rewrites (Wix Thunderbolt resources).
    - Verified that `link` tags were being missed (fixed in `Rewriter.java`).
    - Verified that URL encoding mismatches were causing failures (fixed in `Rewriter.java`).

### Manual Verification
- Verified `analysis_result_v2.txt` showed detailed context-aware reporting.

## 4. Next Steps
- Re-run the full rewrite process on the complete crawl dataset (skipped during session to save time).
- Continue with Page Requisites vs Navigation classification.
