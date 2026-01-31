# Implement Page Resources vs Navigation

## Problem
Currently, `jcrawler` treats all extracted links (`<a href...>`, `<img src...>`, `<script src...>`, etc.) as generic "HyperLinks". This means that if `img src` points to an external domain (e.g., a CDN), it is blocked by the default domain filtering rules unless that external domain is explicitly added to `--accept`.

This differs from standard tools like `wget -p` (page requisites) or browsers, which automatically fetch assets needed to render the page regardless of their origin, while still restricting *navigation* (clicking links) to the target domain.

## Analysis of Other Tools

### Key Distinction: "Navigation" vs. "Page Resources"
Most mature crawlers distinguish between:
1.  **Page Resources (Assets/Embeds)**: Images, CSS, JS, Fonts, PDFs, Media needed to *render* or *complete* the current page context.
2.  **Navigation (Hyperlinks)**: `<a href="...">` tags that lead to a *new* page context via user action.

### Comparison

*   **Wget**:
    *   **Default**: Strict. Won't go off-site.
    *   **With `-p` (`--page-requisites`)**: Downloads all files necessary to display the HTML page (images, css, js), **even if they are on external domains**. It treats them as resources of the page.
    *   **With `-H` (`--span-hosts`)**: Allows *navigation* to traverse to other domains.

*   **HTTrack**:
    *   Designed for mirroring. It has a "get non-HTML files related to a page" rule. It generally fetches external images/css/js by default to ensure the local mirror works, but stops you from browsing *out* to external sites (navigation links).

*   **Heritrix (Archive.org)**:
    *   Uses "Scope" and "Hops".
    *   **Embed Hops**: Usually set to 1. It will fetch any embedded resource (img, script) regardless of domain (0 hops from current page).
    *   **Referral Hops**: Strict for navigation. It won't follow a click to an external site.

*   **Scrapy**:
    *   Typically uses separate pipelines: "ImagePipeline" downloads assets (often from decent CDNs) while the "Spider" logic restricts next-request generation to allowed domains.

## Goal
To match the intelligence of `wget -p`, `jcrawler` must:
1.  **Auto-accept Resources**: Automatically "accept" (allow download of) any URL identified as a resource (found in `src`, `poster`, `style`, or specific file types), regardless of domain (shallow fetch).
2.  **Restrict Navigation**: Only apply the strict "Domain/Accept" filter to `<a href="...">` tags (deep crawl).
