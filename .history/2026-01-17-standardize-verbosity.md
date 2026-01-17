# Session Archive: Standardize Verbosity
**Date:** 2026-01-17
**Issue:** jcrawl ... -vvv did not show debug logs because JCrawler was overriding the global log level with its own default (WARN).

## Process
1.  **Investigation**: Found that CrawlerWorker was calling config.verbosity.configureLoggingLevel(), which explicitly set the org.raisercostin.jcrawler logger.
2.  **Standardization**: Removed the conflicting -V option and Verbosity enum from JCrawler.
3.  **Enhancement**: Added RichCli.setLoggingLevel(category, int) to support granular logging without resetting the whole context.
4.  **Refactor**: Updated JCrawler to provide withVerbosity(int) for tests, delegating to RichCli.
5.  **Build Fix**: Moved RichTest.java and RichTestCli.java from main to 	est. Deleted RichTestCli.java because it had unconfigured dependencies.
6.  **Protocol Update**: Updated sync command to enforce DEVLOG timing and commit message content.

## Results
jcrawl ... -vvv now correctly shows all debug logs. The system is more idiomatic.
