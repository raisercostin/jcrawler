# jcrawler
Crawiling in java 

## Features
- crawler
- [ ] scraping
  - [ ] jsoup convert
  [ ] crawling
  - [x] option to remove query params when searching for new links
  - [x] whitelist from other html
  - [x] strategies - guava traversers
    - [x] depth first
    - [x] breadth first
  - [x] add cache on disk - disk io is faster than network io
    - [x] expiryDuration for now
    - [ ] could implement etag (so call OPTION first and not GET)
  - [ ] do not overwrite on redownload but rename old version
  - [ ] parallelism
    - [ ] hard to generate only what is needed (for example late limiting with take(5) ) because this will impede the performance. It looks like backpropagation would be best? But for maximum throughput you don't want to limit from consumer part.
    - [ ] options
      - parallel streams - will not work since will become essentially a depth-first
      - reactor - flatMap will become essentially a depth-first
      - completable features
      - simple basic executor threads and concurrent set and queue/stack for visited and horizon
        - this will allow in the future to expand it to multiple VMs by partitioning of urls or having cloud visited/horizon structures implementations (database, kafka, message queues)
        - visited can be always detected from stored content (cache)
        - horizon can be always recomputed from stored content
  - [ ] max parallel threads
  - [ ] max calls/second
  - [ ] link seeders/generators
    - [ ] from google
    - [ ] from patterns
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


## Competition

## Development
- To release `mvn release:prepare release:perform -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"`
