# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/org/raisercostin/jcrawler/` contains the core crawler and the CLI entrypoint (`JCrawler`).
- `src/main/java/org/raisercostin/jscraper/` holds scraping helpers used by the crawler.
- `src/main/resources/` contains runtime configs like `logback.xml`.
- `src/test/java/` holds JUnit tests, typically named `*Test.java`.
- `src/test/resources/` contains HTML fixtures and other test data.
- `src/build/` stores formatter configs (Eclipse Java formatter, JSON formatter).
- Repo root includes `pom.xml` (Maven), `package.json` + `scripts.ts` (release and formatting), and `jcrawler.json` (Scoop manifest).

## Build, Test, and Development Commands
- `mvn clean install` builds the project and runs tests.
- `mvn test` runs tests; if tests are skipped by defaults, add `-DskipTests=false`.
- `mvn exec:java -Dexec.mainClass="org.raisercostin.jcrawler.JCrawler" -Dexec.args="--help"` runs the CLI from source.
- `java -jar target/jcrawler-*.jar --help` runs the packaged CLI jar.
- `npm run reformat` applies Maven-based formatting (Eclipse Java formatter, XML formatting).
- `npm run reformat-prettier` formats non-Java assets with Prettier.

## Coding Style & Naming Conventions
- Indentation is 2 spaces with spaces (no tabs); see `.editorconfig`.
- Java formatting follows `src/build/eclipse-2020-06-18--java-code-style-formatter.xml` (indent size 2, tab char space).
- JSON and properties formatting uses `src/build/json-formatter.properties` (indent 2); Prettier uses `tabWidth: 2` and `printWidth: 100`.
- Naming: Java classes use `PascalCase`, methods and fields use `lowerCamelCase`, and tests end with `Test` (example: `JCrawlerTest`).
- When extending existing code, prefer the current style: Vavr collections and Jedio utilities over raw Java collections and IO helpers.

## Testing Guidelines
- Frameworks: JUnit 5 and AssertJ (`org.junit.jupiter`, `org.assertj`).
- Place tests under `src/test/java` and keep the `*Test.java` naming pattern.
- Fixtures belong in `src/test/resources` (HTML samples already exist).
- Some tests hit live URLs; document external dependencies and tag or disable as needed.

## Commit & Pull Request Guidelines
- Commit history uses short, imperative messages like `improve crawler`, `fix`, `reformat`, and release plugin messages; follow the same concise style.
- For release work, keep Maven release plugin messages (`[maven-release-plugin] ...`) intact.
- PRs should include a short summary, tests run (or reason for skipping), and any CLI behavior changes or new flags. Link issues when applicable.

## Configuration and Runtime Notes
- Default crawl output goes to the project dir (CLI default `.jcrawler`), with content files plus `.meta` and cache artifacts; avoid committing generated crawl data.
- The Scoop manifest is `jcrawler.json`; update it only when publishing new CLI binaries.