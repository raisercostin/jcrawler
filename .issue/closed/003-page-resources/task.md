# Task: Implement Page Resources

- [ ] Implement Page Resources vs Navigation
    - [x] Update `HyperLink` and `LinkMatcher` to support `isResource`
    - [x] Update regex definitions to classify links
    - [x] Refine resource detection by file extension (pdf, zip, etc.)
    - [x] Update extraction logic to populate `isResource`
    - [x] Update `CrawlerWorker` to auto-accept resources
    - [x] Cleanup logging verbosity (INFO = pages/resources + cache status only)
    - [x] Localize inline assets (scripts, styles, style attributes) <!-- id: 4 -->
    - [x] Fix test failures in LocalizationInvariantTest <!-- id: 5 -->
    - [x] Verify with `PageResourcesTest` <!-- id: 29 -->

- [x] Localization of Inline Assets
    - [x] Implement `rewriteScriptContents` in `Localizer.java`
    - [x] Implement `rewriteStyleContents` in `Localizer.java`
    - [x] Verify no external Wix URLs remain with `LocalizationInvariantTest`
