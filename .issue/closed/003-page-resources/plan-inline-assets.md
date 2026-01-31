# Localization Expansion: Inline Scripts and Styles

Ensure all URLs, including those hidden in JSON blobs in `<script>` tags or CSS in `<style>` tags, are localized for a true offline experience.

## Proposed Changes

### [Component] Localizer

#### [MODIFY] [Localizer.java](file:///d:/home/raiser/work/namek-jcrawl/src/main/java/org/raisercostin/jcrawler/Localizer.java)
- Add `rewriteScriptContents(Document doc, PathLocation source, String baseUrl)`
- Add `rewriteStyleContents(Document doc, PathLocation source, String baseUrl)`
- Update `localizeHtml` to call these new methods.
- Implementation detail: Iterate through `globalUrlMap` and perform global string replacement in script/style tags.

## Verification Plan

### Manual Verification
- Run localization on the existing `.jcrawler` data.
- Grep for the reported Wix URL in the output `.local-view-verify` folder.
- Ensure the URL is replaced with a relative path (e.g., `_files/static.wixstatic.com/...`).
