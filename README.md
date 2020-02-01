[![Build Status](https://github.com/Jaitl/cloud-crawler/workflows/Build/badge.svg?branch=master)](https://github.com/Jaitl/cloud-crawler/actions?workflow=Build)
[![Release Status](https://github.com/Jaitl/cloud-crawler/workflows/Release/badge.svg?branch=master)](https://github.com/Jaitl/cloud-crawler/actions?workflow=Release)
[![Version](https://img.shields.io/github/release/Jaitl/cloud-crawler.svg?label=Version)](https://github.com/Jaitl/cloud-crawler/releases)
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
[![GitHub release](https://img.shields.io/github/release/Jaitl/cloud-crawler.svg?label=version)](https://bintray.com/jaitl/cloud-crawler/worker)
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
