# Project Context: jcrawler

## Project Overview

`jcrawler` is a Java-based web crawler tool and fluent library designed for efficient traversing, downloading, and caching of web content. It supports parallel crawling, custom link extraction (handling `srcset`, `style`, `script`, etc.), and local caching with metadata.

**Key Technologies:**
*   **Language:** Java 17+
*   **Build System:** Maven
*   **CLI Framework:** Picocli
*   **HTTP Client:** Java 11+ `java.net.http.HttpClient`
*   **Libraries:** Vavr (functional), Guava, Jackson (JSON), Jsoup (HTML parsing), Jedio (File/IO abstraction), Jedi-Nodes (Structure mapping).
*   **Release Automation:** Node.js scripts (`scripts.ts`) using `ts-node`.

## Building and Running

### Prerequisites
*   Java JDK 17 or higher
*   Maven
*   Node.js (for release scripts and formatting)

### Key Commands

**Build:**
```bash
mvn clean install
```

**Run CLI (from source):**
You can run the main class directly using Maven or your IDE.
```bash
mvn exec:java -Dexec.mainClass="org.raisercostin.jcrawler.JCrawler" -Dexec.args="--help"
```

**Run CLI (Packaged):**
After building, the jar can be executed:
```bash
java -jar target/jcrawler-*.jar --help
```

**Release Management:**
The project uses `scripts.ts` for release tasks.
*   `npm run release-prepare`: Prepares the release (likely updates versions).
*   `npm run release-perform-local`: Performs a local release to a specified repository.
*   `npm run normalize-pom`: Normalizes the `pom.xml` structure.

**Formatting:**
*   `npm run reformat-prettier`: Formats code using Prettier.
*   Maven also has a `formatter-maven-plugin` configured to use Eclipse code style settings found in `src/build/`.

## Codebase Structure

*   **`src/main/java/org/raisercostin/jcrawler/JCrawler.java`**: The main entry point and CLI definition. Configures the crawler, handles arguments, and initiates the `CrawlerWorker`.
*   **`src/main/java/org/raisercostin/jcrawler/CrawlerWorker`**: (Inner class in `JCrawler.java`) Contains the core crawling logic: fetching URLs, extracting links, and managing the queue.
*   **`src/main/java/org/raisercostin/jcrawler/Generators.java`**: Handles URL generation patterns (e.g., `{1-3}`, `{opt1|opt2}`).
*   **`src/main/java/org/raisercostin/jscraper/`**: Contains scraping related logic, likely complementary to the crawler.
*   **`.jcrawler/`**: Default directory for storing crawled content and configuration (can be overridden via `-p`).
*   **`scripts.ts`**: TypeScript script for build and release automation.

## Development Conventions

*   **Code Style:** strict adherence to defined Eclipse formatter profiles and Prettier.
*   **Testing:** JUnit 5 is used. Tests are located in `src/test/java`.
*   **Logging:** SLF4J with Logback.
*   **Data Structures:** Heavy usage of Vavr collections (`Seq`, `Set`, `Map`) and functional patterns instead of standard Java collections where possible.
*   **File I/O:** Uses `jedio` library for rich file operations (`Locations.path(...)`, `asWritableFile()`, etc.).
