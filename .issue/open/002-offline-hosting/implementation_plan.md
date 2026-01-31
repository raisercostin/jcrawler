# Offline Link Localization Plan

## Goal Description
Enable `jcrawler` to produce "hostable" local copies of websites by rewriting links in downloaded HTML files. Currently, absolute paths (e.g., `/css/style.css`) break when the crawled site is hosted in a subdirectory or opened directly from the filesystem. This feature will "localize" these links to be relative to the current file (e.g., `../css/style.css`), ensuring the site renders correctly offline.

Crucially, this system must support **multi-site localization**. If a user crawls `site1.com` and `site2.com`, dependencies between them (e.g., `site1` linking to `site2`) must be rewritten to point to the local relative path of `site2`, ensuring the entire mesh of sites works offline.

## User Review Required
> [!IMPORTANT]
> **Regex vs Jsoup**: The current crawler uses Regex for link extraction (performance-focused). For this transformation, I propose using **Jsoup** to ensure robust handling of HTML attributes, which is safer but slower. This will be a post-processing step or a separate "transform" command, disjoint from the high-speed crawl.

> [!NOTE]
> **New Command**: I plan to add a `localize` command (e.g., `jcrawl localize --dirs .jcrawler/site1,.jcrawler/site2 --output .jcrawler/offline`) rather than modifying the core crawl loop. This keeps the crawler fast and allows localization to be run on existing crawls.

## Proposed Changes

### `src/main/java/org/raisercostin/jcrawler`

#### [NEW] [Localizer.java](file:///d:/home/raiser/work/namek-jcrawl/src/main/java/org/raisercostin/jcrawler/Localizer.java)
- A new class responsible for processing a set of crawled directories.
- **Inputs**: A list of source directories (representing different sites/domains).
- **Logic**:
    1. **Index Phase**: Scan all input directories to build a "Global URL Map" (`Original URL -> Local Relative Path`).
        - E.g., `https://site1.com/page` -> `site1/page.html`.
        - E.g., `https://site2.com/asset` -> `site2/asset.jpg`.
    2. **Process Phase**: Iterate through all `.html` files in all directories.
    3. **Rewrite Logic**: For each link (`href`, `src`):
        - If it matches a known entry in the "Global URL Map" (even if cross-domain):
            - Calculate relative path from *current file* to *target file*.
            - Replace attribute.
        - If it's an absolute path (`/foo`) related to the current domain:
            - Relativize to local root.
    4. **Output**: Save modified files to a unified `offline/` directory, preserving the structure `offline/site1/`, `offline/site2/`.

#### [MODIFY] [JCrawler.java](file:///d:/home/raiser/work/namek-jcrawl/src/main/java/org/raisercostin/jcrawler/JCrawler.java)
- Add `localize` CLI command/mixin.
- Arguments:
    - `--inputs`: Comma-separated list of project directories.
    - `--output`: Destination directory for the offline version.
- Wire up `Localizer` invocation.

## Verification Plan

### Automated Tests
- Create a new test `LocalizerTest.java`.
- **Test Case 1**: Simple relative link.
    - Input: `page.html` with `<a href="/about">`.
    - Expected: `<a href="about/index.html">`.
- **Test Case 2**: Cross-site linking.
    - Input: `site1/index.html` with `<a href="https://site2.com/foo">`.
    - Context: `site2/foo.html` exists.
    - Expected: `<a href="../site2/foo.html">`.

### Manual Verification
1. Crawl `www.projects-mobility.com` (site1).
2. Crawl a linked site (site2) if applicable, or simulate one.
3. Run localization:
    - `jcrawl localize --inputs .jcrawler/site1,.jcrawler/site2 --output .jcrawler/offline`
4. Host the `offline` folder:
    - `deno run ... .jcrawler/offline`
5. Verify:
    - Site1 loads correctly.
    - Clicking a link to Site2 opens the local Site2 copy.
