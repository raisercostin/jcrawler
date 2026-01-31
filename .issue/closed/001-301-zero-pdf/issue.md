---
id: 001
title: 301 Redirect results in 0-byte PDF files
status: open
author: Gemini CLI
date: 2026-01-17
---

### Problem
Crawler produces 0-byte PDF files when a 301 redirect is encountered. It saves the redirect response body (which is empty) instead of following the redirect to the final content.

Example:
`.jcrawler\www.projects-mobility.com\_files\ugd\c3db1c_038d1a9efafc4c73ac0707575e5c28a6.E301.pdf` has 0 size.

The URL: `https://www.projects-mobility.com/_files/ugd/c3db1c_038d1a9efafc4c73ac0707575e5c28a6.pdf` exists and is downloadable in browser.

### Reproduce
1. Crawl `https://www.projects-mobility.com/_files/ugd/c3db1c_038d1a9efafc4c73ac0707575e5c28a6.pdf`.
2. Observe that `jcrawler` creates a file with `.E301.pdf` extension and 0 bytes.

### Evidence
- Filename pattern `.E301.pdf` indicates status code 301 was detected.
- Java `HttpClient` defaults to `Redirect.NEVER`.
- `curl.exe -I` confirms 301 to `filesusr.com` (Wix CDN).

### Resolution
- Implemented `isRedirect` flag in `HyperLink` to identify links extracted from redirect headers.
- Added a `log.warn` in `JCrawler.accept` to notify the user when a redirect target is blocked by the hostname filter.
- This maintains site boundaries while providing clear feedback on missing content.

## Comments
- **2026-01-17 (Gemini CLI)**: Initial analysis identified that the 0-byte file is the literal body of the 301 response. The redirect target is being filtered out by `accept` logic because it's on an external domain.
- **2026-01-17 (Gemini CLI)**: Proposed initial resolutions:
  - Enable `Redirect.NORMAL` in `HttpClient`.
  - Refine `download` logic to skip body handler for 3xx and follow manually.
  - Ensure `Slug` generation handles followed redirects correctly to save final body under intended path.
- **2026-01-17 (Gemini CLI)**: Attempted "Automatic Trust Approach": add `trusted` flag to `HyperLink` to bypass filter for redirects if source was accepted. This was **REJECTED** because it violates the "copy specific site" principle. External domains (CDNs) must be explicitly accepted by the user.
- **2026-01-17 (Gemini CLI)**: Final accepted approach: Add warning for ignored redirects. User is notified via log that they need to update `--accept` if the content is required.
