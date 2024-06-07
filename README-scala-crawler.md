A crawler in scala.
Deprecated: see jcrawler

# Design

The abstractions:

- Domain & Implementations
  - Content
    - FileWIthResponseHeaderContent
  - Link
    - RawLink
- Strategies & Implementations
  - LinkDownloader - produce a persisted Content out of Link
    - FileLinkDownloader
  - ContentDownloader - produce a Content out of Link
    - deprecated: JediIoCOntentDownloader
    - BigFileDownloader
  - LinkProducer - parses a Content and extracts Links
    - ContentLinkProducer - extract links from content
    - IteratorLinkProducer - generates new links from a range
  - SlugGenerator - generates a slug for a url
    - FlatSlugGenerator
  - FileDistributor - strategy to store multiple contents in the filesystem
    - deprecated: VersionedFileDistributor
    - HugeFileDistributor

# TODO

- Maybe in a folder (to be similar with emails?). Multiple calls to same thing with different accept type can be saved in same folder.
- Download a little faster from same server? Now is pure serial?
- check and get inspired from `wget` and `curl` format of downloading stuff
  - `curl -sv legislatie.just.ro/Public/DetaliiDocument/191357 >191357.download`
  - `wget http://legislatie.just.ro/Public/DetaliiDocument/191357 -vd`
- Checks meta length to compare with actual file.
- Detects identical content and replace it with links?
- Detect collisions and report them: if in the same session the same slug is generated.
- create PageIdExtractor from url. The simplest one ignores the fragment. A more convoluted one also ignores www and http/https protocol and any permanent redirect.
- download from multiple hosts (systems share current links)
- download strategy: breath first or deep first
- if stopped and restarted continue downloading from where it left. this means links already processed should be marked as such (special folders or markers in meta or name)
- Resume stopped download

- speedup by downloading several links in parallel for same domain/origin
- follow redirects
  - return Location from meta as links
  - download recursively all redirects (while caching all results)
- make it work at
  - https://loteries.lotoquebec.com/en/lotteries/lotto-6-49?annee=2017&widget=resultats-anterieurs&noProduit=212#res
- protocols
  - include: http/https/others
  - exclude: irc
  - others
- sublinks - links in a Content that should have a slug determined by parent and not their own. For example
  Page content from http\://legislatie.just.ro/Public/DetaliiDocumentAfis/180244 slug `ro_just_legislatie--http--public--detaliidocumentafis--180244.html` contains link to `/Public/FormaPrintabila/00000G01X8ZU3IRBU083S54K4EOO99IC` that is generated on this specific request. On subsequent requests the printable form will have a differnt link. All other subsequent scans will generate new and new `FormaPrintabila` links but they are actually the same content. If you know this kind of details for a site is better to clarify it and generate the children slug out of the parent one.
- Meta file should contain timestamps? Should be ordered: url, request, response? Should be yaml?
- download images and other content?

# History

## 2018-08-02

- filter domains
- reimplemented crawl state: - / todo / finished

## 2018-07-26

- filter domains
- crawl state: - / todo / finished

## 2018-07-22

- BigFileDownloader should also store the request parameters
- save request/response header with body
- return the InputStream not a local file. The client decides if he wants to copy it in a local file
- don't stop on errors at `downloadLink`
  - save meta and clarify that there is an error client side
  - as fallback printStackTrace in log and continue to extract links from other contents
- If the file name exceeds 256
  - FileDistributor changes slug to incorporate the hash if the file name exceeds the [256 limit for windows10-ntfs-exFat/linux-vFat/osx-hfs](https://en.wikipedia.org/wiki/Filename#Comparison_of_filename_limitations)
  - a url can go to 2000 and more chars - https://stackoverflow.com/questions/417142/what-is-the-maximum-length-of-a-url-in-different-browsers
- redownload if necessary
  - If meta exists and file doesn't the meta should be ignored and re-downloaded with the new content.
  - If meta exists and file is too short the meta should be ignored and re-downloaded with the new content.
- meta keys must be case insensitive `response.Content-Type` or `response.content-type`: https://stackoverflow.com/questions/5258977/are-http-headers-case-sensitive

## 2018-07-21

- Extract ContentDownloader strategy
- Implement BigFileDownloader as a ContentDownloader strategy better than JediIoContentDownloader
- Handle redirects if asked by returning the parent Content's. The request/response must be stored to preempt new requests

## 2018-07-19

- With new links download new content and start over
- How to recursive download (300 000 pages ?)
  A link is either `toCrawl` (just discovered) or `crawled`

## 2018-07-18

- From a `[slug].meta` file the original `[slug].html` should be found based on info stored in meta:
  - Content-Type (already processed)
  - extension
- For a `[slug].meta` file the original url should be found in order to be able to interpret relative links in content
  - url
- With slug and extension you get the contentLocation
- with contentLocation and url you get the new links

## 2018-07-17

- Separate concepts in traits
- Use response.Content-Type for extension of body. Use cache if `[slug].meta` exists.
- cache: check if file exists before download.
- Use slug injective function to check for cache
- Continues crawling if interrupted in the middle of something.
- FlatSlugGenerator has problems with servers with big number of files so you also need a FileDistributor (a strategy that returns a location for a slug)
  1. VersionedFileDistributor - make a free folder like in 1 and then go to the nesting strategy as described here - https://stackoverflow.com/questions/2994544/how-many-files-in-a-directory-is-too-many-on-windows-and-linux
     Then if you search you lookup in two locations: the simple one and the folder one. The folder one should use a hash and group by that hash.
     Like first level AA, AB, AC, ..., AZ, ... ZA ... ZZ 26x26=676 folders in a folder.
     If more than a threshold number of files are downloaded put them in a second folder. This complicates the check if file exists because you must go in all folders.
     No matter how the files are split it should work. If you manually copy some files in a versioning folder should be ok. First start without version like in 1) but if it goes beyond that change it.
  2. HugeFileDistributor - search in current folder if not exists take first/next two chars from a hash of the slug and search in that folder.
     The problem with this distributor is that
     - a file moved from "his" folder in other place that it's ancestors will not be found anymore.
     - if the maxFilesPerFolder is increased the older files are not found anymore

# Alternative crawlers

- java
  - https://github.com/yasserg/crawler4j
- https://scrapy.org/ : python
- https://www.google.ro/search?q=scala+crawler
- http://java-source.net/open-source/crawlers
- nutch - http://nutch.apache.org/
- sparkler - https://github.com/USCDataScience/sparkler
  - https://www.slideshare.net/thammegowda/sparkler-spark-crawler
- http://foat.me/articles/crawling-with-akka/
  - source: https://github.com/Foat/articles/tree/master/akka-web-crawler
- https://github.com/dyweb/scrala
- cloud
  - https://scrapinghub.com/scrapy-cloud
- wget
  `wget -r --save-headers --page-requisites http://www.dcsi.eu`

## File Systems

Expose data over

- https://github.com/jimmidyson/kuisp
- other file systems
  - https://www.gluster.org/install/ to share as CIFS/Samba

Streaming with apache kafka (https://kafka.apache.org/)

# Development

## Build

Run `mvn eclipse:eclipse`, switch to scala 2.11
