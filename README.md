# jcrawler
Crawiling in java 

## Features

- [ ] scraping
  - [ ] jsoup convert
- [ ] crawling
  - [ ] breadth search - now is recurisve depth search
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
    - [ ] query removal
    - [ ] protocol/schema filter
  - [x] adding meta
    - [x] http meta
    - [x] hyperlinks meta file
    - [x] add pluggable config of meta 


## Competition

## Development
- To release `mvn release:prepare release:perform -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"`
