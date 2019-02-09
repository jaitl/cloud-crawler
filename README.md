[![Build Status](https://travis-ci.org/Jaitl/cloud-crawler.svg?branch=master)](https://travis-ci.org/Jaitl/cloud-crawler)
[![Coverage Status](https://coveralls.io/repos/github/Jaitl/cloud-crawler/badge.svg?branch=master)](https://coveralls.io/github/Jaitl/cloud-crawler?branch=master)
# cloudCrawler
A framework for building a distributed, highload system for crawling open data.

### Run
1. Run two master node:
    ```
    ./gradlew :master:run -DMASTER_PORT=2551
    ./gradlew :master:run -DMASTER_PORT=2552
    ```
2. Run worker nodes:
    ```
    ./gradlew :simple-worker:run -DWORKER_PORT=2561
    ./gradlew :simple-worker:run -DWORKER_PORT=2562
    ```

### Worker dependency
[bintray.com](https://bintray.com/jaitl/cloud-crawler/)
#### sbt
```
resolvers += "Cloud Crawler Repository" at "https://dl.bintray.com/jaitl/cloud-crawler",
libraryDependencies += "com.github.jaitl.crawler" %% "worker" % version
```

#### gradle
repo:
```
repositories {
    maven {
        url  "https://dl.bintray.com/jaitl/cloud-crawler" 
    }
}
```
dependency:
```
compile 'com.github.jaitl.crawler:worker_2.12:version'
```
