[![Build Status](https://travis-ci.org/Jaitl/cloud-crawler.svg?branch=master)](https://travis-ci.org/Jaitl/cloud-crawler)
[![Coverage Status](https://coveralls.io/repos/github/Jaitl/cloud-crawler/badge.svg?branch=master)](https://coveralls.io/github/Jaitl/cloud-crawler?branch=master)
# cloudCrawler
A framework for building a distributed, highload system for crawling open data.

## Run
1. Run two master node:
    ```
    sbt -DMASTER_PORT=2551 master/run
    sbt -DMASTER_PORT=2552 master/run
    ```
2. Run worker nodes:
    ```
    sbt -DWORKER_PORT=2561 simple-worker/run
    sbt -DWORKER_PORT=2562 simple-worker/run
    ```

## Simple task
1. MongoDB
```
{
    "_id" : ObjectId("5c5aea3dec37b20006c9c492"),
    "taskType" : "HabrTasks",
    "taskData" : "438886",
    "taskStatus" : "taskInProgress",
    "attempt" : 0,
    "lastUpdate" : NumberLong(1549465631389)
}
```

## Worker dependency

[bintray.com](https://bintray.com/jaitl/cloud-crawler/)
### sbt
```
resolvers += "Cloud Crawler Repository" at "https://dl.bintray.com/jaitl/cloud-crawler",
libraryDependencies += "com.github.jaitl.crawler" %% "worker" % version
```

### gradle
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
