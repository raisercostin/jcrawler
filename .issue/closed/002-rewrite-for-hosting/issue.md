# 002 Offline Hosting Structure

## Status
New

## Description
[User Report]
running jcrawl against www.projects-mobility.com creates a structure that cannot be hosted (i do that as `deno run --allow-net --allow-read --allow-sys https://deno.land/std@0.224.0/http/file_server.ts -p 4507`) cannot be hosted because urls even locally on the site are sometimes absolute. What is customary to do for this? Maybe create a jcrawl transform to create a local copy that is relative to current dir? (in this way to have also the original)? Also maybe if i crawl site1 and also refers site2 and i do this "relativizations" any site1 links to site2 are transformed into localsite/prj1/site1/... and localsite/prj1/site2/... and in this way multiple sites crawled can coexist under some paths? prj1/ prj2/  eventually they could in the future consolidate like rsync where you cand pass dest place that can be refered via links. what is customiary to do: waybackmachine on archive.org and other crawlers from python and so on? put all this also in a local .issue file (read the .gene/README.md initially).

## Analysis
Current `jcrawler` saves content as received (raw HTML).
- **Absolute Paths**: Links beginning with `/` (e.g., `/assets/style.css`) resolve to the filesystem root (e.g., `C:\assets\style.css`) when hosted locally via a simple file server, failing to load if the site root is a subdirectory (like `.../002-offline-hosting/...`).
- **Absolute URLs**: Links to `https://target.com/page` point to the live web, bypassing the local crawl.
- **Cross-site links**: As noted by the user, links between crawled sites (site1 -> site2) should ideally point to the local copy of site2.

`Slug.java` handles mapping URLs to filesystem paths (e.g., escaping characters), but it does not modify the *content* of the files.
`JCrawler.java` extracts links using Regex (Line 312+) but does not perform content replacement during the crawl.

## Proposed Solution
Implement a post-processing "localization" step (or a "transform" mode).
- **Relativization**: Convert all internal links (absolute paths and absolute URLs matching the domain) to relative paths (e.g., `../../assets/style.css`).
- **Cross-site linking**: If site2 is also crawled/localized, rewrite links to point to site2's local relative path.
- **Technology**: Use Jsoup (already a dependency) to safely parse and modify HTML attributes (`href`, `src`, `srcset`).

## Customary Practices
- **Wget (`-k` / `--convert-links`)**: Converts links to point to local files for offline viewing.
- **HTTrack**: specialized for offline browsing, heavily rewriting links.
- **Wayback Machine**: Uses server-side rewriting (clients see rewritten links dynamically) rather than static file rewriting, though the underlying storage is often WARC (raw).

## Future
- Consolidation of multiple crawls (rsync-style) where `site1` and `site2` coexist in a standard structure.
