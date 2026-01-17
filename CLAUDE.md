# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run CLI from source
mvn exec:java -Dexec.mainClass="org.raisercostin.jcrawler.JCrawler" -Dexec.args="--help"

# Run packaged CLI
java -jar target/jcrawler-*.jar <url>

# Format code (Java via Maven Eclipse formatter)
npm run reformat

# Format non-Java assets (Prettier)
npm run reformat-prettier

# Release
npm run release-prepare
npm run release-perform-local -- --releaseVersion <version>
```

## Architecture

**jcrawler** is a Java web crawler with CLI and library interfaces built on:
- **Picocli** for CLI argument parsing
- **Java 11+ HttpClient** for HTTP requests
- **Vavr** for functional collections (`Seq`, `Set`, `List`, `Option`)
- **Jedio** for file/IO abstraction (`Locations`, `DirLocation`, `WritableFileLocation`)
- **Guava Traverser** for graph traversal strategies

### Core Components

- `JCrawler.java` - Main entry point, CLI definition, and orchestration. Contains `CrawlerWorker` inner class with crawling logic
- `CrawlerWorker` - Handles URL fetching, link extraction, caching, and queue management
- `HyperLink.java` - Represents a crawled link with metadata (depth, source, directive)
- `Slug.java` - URL-to-filesystem path conversion with multiple encoding strategies
- `Generators.java` - URL pattern expansion (`{1-3}`, `{opt1|opt2}`)
- `ParallelGraphTraverser.java` - Custom parallel breadth-first traversal implementation

### Crawl Data Structure

Output goes to `.jcrawler/` (configurable via `-p`):
- Content files stored with slugified paths
- `.meta.json` - HTTP metadata (headers, status, URL)
- `.links.json` - Extracted hyperlinks from content
- `.index/` - Symlinks mapping URL hashes to content files

### Traversal Types

`TraversalType` enum controls crawling order:
- `PARALLEL_BREADTH_FIRST` (default) - Concurrent with token-based rate limiting
- `BREADTH_FIRST`, `DEPTH_FIRST_PREORDER`, `DEPTH_FIRST_POSTORDER` - Sequential via Guava

### Link Extraction

`extractLinksFromContent()` uses regex patterns to extract links from:
- `<a href>`, `<link href>`, `<img src/srcset>`, `<script src>`
- CSS `url()` in style attributes
- `robots.txt` directives
- XML sitemaps `<loc>` elements

## Code Style

- 2-space indentation (no tabs)
- Prefer Vavr collections over standard Java collections
- Use Jedio `Locations.*` for file operations
- Tests use JUnit 5 + AssertJ, named `*Test.java`
