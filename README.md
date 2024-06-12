# jcrawler

Crawler tool and java fluent library.

## Install

```cli
scoop install https://github.com/raisercostin/jcrawler/raw/master/jcrawler.json
```

## CLI Usage

```cli
jcrawl

Usage: jcrawl [-hV] [--debug] [--expire=<cacheExpiryDuration>]
              [--maxConnections=<maxConnections>] [--maxDocs=<maxDocs>]
              [-o=<cache>] [-v=<verbosity>] [-p=<protocols>]... urls [COMMAND]
Crawl tool.
      urls                  Urls to crawl. If urls contain expressions all
                              combinations of that values will be generated:
                            - ranges like {start-end} that will be expanded to
                              all values between start and end.
                            - alternatives like {option1|option2|option3} and
                              all of them will be used

                            For example
                              https://namekis.com/{docA|doc2}/{1-3}
                            will generate the following urls:
                              https://namekis.com/docA/1
                              https://namekis.com/docA/2
                              https://namekis.com/docA/3
                              https://namekis.com/doc2/1
                              https://namekis.com/doc2/2
                              https://namekis.com/doc2/3

      --debug               Show stack trace
      --expire=<cacheExpiryDuration>
                            Expiration as a iso 8601 format like P1DT1S.
                             Full format P(n)Y(n)M(n)DT(n)H(n)M(n)S
                            See more at https://www.digi.
                              com/resources/documentation/digidocs/90001488-13/r
                              eference/r_iso_8601_duration_format.htm
  -h, --help                Show this help message and exit.
      --maxConnections=<maxConnections>

      --maxDocs=<maxDocs>
  -o, --outdir=<cache>      Dir to write crawled content
  -p, --protocol=<protocols>
                            Set the protocol: HTTP11, H2, H2C.
                              Default: null
  -v, --verbosity=<verbosity>
                            Set the verbosity level: NONE, ERROR, WARN, INFO,
                              DEBUG, TRACE.
                              Default: INFO
  -V, --version             Print version information and exit.
Commands:
  generate-completion  Generate bash/zsh completion script for jcrawl.
```

## Library Usage

### Maven

#### Dependency

See released versions at https://github.com/raisercostin/maven-repo/tree/master/org/raisercostin/jcrawler/0.1

```
<dependency>
  <groupId>org.raisercostin</groupId>
  <artifactId>jcrawler</artifactId>
  <version>0.1</version>
</dependency>
```

#### Repository

```
<repository>
  <id>raisercostin-github</id>
  <url>https://github.com/raisercostin/maven-repo/tree/master</url>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```

### Minimal config

```java
@Test
void raisercostinOrg() {
  JCrawler crawler = JCrawler.crawler().start("http://raisercostin.org");
  assertThat(crawler.crawl().take(6).mkString("\n")).isEqualTo("""
      http://raisercostin.org
      https://raisercostin.org/
      https://raisercostin.org/bliki
      https://raisercostin.org/talk
      https://raisercostin.org/2017/04/24/GhostInTheShell
      https://raisercostin.org/2017/04/18/PalidulAlbastruPunct""");
}
```

This will create in a local dir `.crawl` the files with content but also with metadata of the call like

```json
{
  "url": "http://raisercostin.org",
  "method": "GET",
  "statusCode": "301 MOVED_PERMANENTLY",
  "statusCodeValue": 301,
  "responseHeaders": {
    "Date": "Sun, 09 Jun 2024 17:10:31 GMT",
    "Content-Type": "text/html",
    "Content-Length": "167",
    "Connection": "keep-alive",
    "Cache-Control": "max-age=3600",
    "Expires": "Sun, 09 Jun 2024 18:10:31 GMT",
    "Location": "https://raisercostin.org/",
    "Report-To": "{\"endpoints\":[{\"url\":\"https:\\/\\/a.nel.cloudflare.com\\/report\\/v4?s=ef0N%2FG9ZblY3p9bORP2XCblaVc2QF7GUzHy4kkxKDYRUi8g1fiuwL5MJNqkg9tJxE%2Fn3eTHiAcje6Ie5W%2FyouUYajv2DRl917%2Fn3wSIinYaaKybL5%2F3TEkGHeOgD9izkbLUV\"}],\"group\":\"cf-nel\",\"max_age\":604800}",
    "NEL": "{\"success_fraction\":0,\"report_to\":\"cf-nel\",\"max_age\":604800}",
    "Vary": "Accept-Encoding",
    "Server": "cloudflare",
    "CF-RAY": "8912bc506e34b9db-OTP",
    "alt-svc": "h3=\":443\"; ma=86400"
  },
  "requestHeaders": {}
}
```

### More complex

```java
static void main(){
  JCrawler crawler = JCrawler
    .crawler()
    .start("https://legislatie.just.ro/Public/DetaliiDocument/1")
    .withCache(Locations.dir("d:\\home\\raiser\\work\\_var_namek_jcrawl\\scan4-just").mkdirIfNeeded())
    .withFiltersByPrefix(
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/1",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/2",
      "https://legislatie.just.ro/Public/DetaliiDocumentAfis/3")
    .withCacheExpiryDuration(Duration.ofDays(100))
    .withMaxDocs(5)
    .withMaxConnections(3)
    .withProtocol(HttpProtocol.HTTP11);
  RichIterable<String> all = crawler
    .withMaxDocs(1005)
    .withMaxConnections(3)
    //.withCacheExpiryDuration(Duration.ofSeconds(1))
    .withTraversalType(TraversalType.PARALLEL_BREADTH_FIRST)
    //.withGenerator("https://legislatie.just.ro/Public/{DetaliiDocument|DetaliiDocumentAfis}/{1-3}")
    .withGenerator("https://legislatie.just.ro/Public/DetaliiDocumentAfis/{1000-1010}")
    .crawl();

  String actual = all.sorted().mkString("\n");
  assertThat(actual).isEqualTo(
    """
        https://legislatie.just.ro/Public/DetaliiDocument/1
        https://legislatie.just.ro/Public/DetaliiDocument/131185
        https://legislatie.just.ro/Public/DetaliiDocument/26296
        https://legislatie.just.ro/Public/DetaliiDocumentAfis/129268
        https://legislatie.just.ro/Public/DetaliiDocumentAfis/131085""");
}
```

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
- [ ] Versioned slug. When changing encoding old crawlers cache result will not work. Best to keep all slug strategies and use one by one by priority.

## History

- 2024-06-12
  - log cache local file on download
  - extension from content type if not exists in url
    - cache hit will be detected by hash that doesn't consider extension?
  - no logs if not enabled by command line
  - use blocking standard java http client (in java21 virtual threads will help)
  - ignore #fragments from urls
- 2024-06-11
  - cache for servers with errors
- [x] 2024-06-10
  - open source on github.com
  - fat jar release
  - installer via scoop
  - [ ] allowedDomain is computed based on the start urls
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

- <https://scrapy.org/>

## Development

- To release
  - ```cli
    npm run release-prepare
    npm run release-perform-local -- --releaseVersion 0.86
    ```
