# jcrawler

Crawiling in java

## Features

- [x] crawler
  - [x] option to remove query params when searching for new links
  - [x] whitelist from other html
  - [x] strategies
    - [x] guava traversers
      - [x] depth first
      - [x] breadth first
    - [x] custom parallel breadth first traversal
      - [x] max parallel threads
      - [x] parallelism
        - [x] Added `TraversalType.PARALLEL_BREADTH_FIRST` that tries to download in breadth style, but because of parallelism the order is not well defined. This also respects the maxDocuments limit
          - [x] This is using a BlockingQueue for visited and a set for horison.
        - [-] hard to generate only what is needed (for example late limiting with take(5) ) because this will impede the performance. It looks like backpropagation would be best? But for maximum throughput you don't want to limit from consumer part.
        - [-] options
          - parallel streams - will not work since will become essentially a depth-first
          - reactor - flatMap will become essentially a depth-first
          - completable features
          - simple basic executor threads and concurrent set and queue/stack for visited and horizon
            - this will allow in the future to expand it to multiple VMs by partitioning of urls or having cloud visited/horizon structures implementations (database, kafka, message queues)
            - visited can be always detected from stored content (cache)
            - horizon can be always recomputed from stored content
  - [x] add cache on disk - disk io is faster than network io
    - [x] expiryDuration for now
    - [ ] could implement etag (so call OPTION first and not GET)
  - [ ] link extractors
    - [x] html
    - [x] http header redirects: Location
    - [ ] robots.txt
    - [ ] sitemap xml gz.xml
    - [ ] feed
  - [ ] link/content filtering
    - [x] authority: domain/subdomain
    - [ ] depth
    - [ ] content type
    - [ ] protocol/schema filter
  - [x] adding meta
    - [x] http meta
    - [x] hyperlinks meta file
    - [x] add pluggable config of meta

## History

- [x] 2024-05-06 - added generators like: https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}
  - Or between `{` abd `}` separated with `|`
  - Range between `{` abd `}` separated with `-`
  - Anything else is an and
  - This will generate
    ```
    https://legislatie.just.ro/Public/DetaliiDocument/1
    https://legislatie.just.ro/Public/DetaliiDocument/2
    https://legislatie.just.ro/Public/DetaliiDocument/3
    https://legislatie.just.ro/Public/DetaliiDocumentAfis/1
    https://legislatie.just.ro/Public/DetaliiDocumentAfis/2
    https://legislatie.just.ro/Public/DetaliiDocumentAfis/3
    ```

## TODO

- [ ] add CLI
- [ ] crawling
  - [ ] do not overwrite on redownload but rename old version
  - [ ] max calls/second
  - [ ] link seeders/generators
    - [ ] from google
    - [ ] from patterns
- [ ] scraping
  - [ ] jsoup convert

## Competition

## Development

- To release
  - ```cli
    npm run release-prepare
    npm run release-perform-local -- --releaseVersion 0.86
    ```
