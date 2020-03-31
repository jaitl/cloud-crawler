[![Build Status](https://github.com/Jaitl/cloud-crawler/workflows/Build/badge.svg?branch=master)](https://github.com/Jaitl/cloud-crawler/actions?workflow=Build)
[![Release Status](https://github.com/Jaitl/cloud-crawler/workflows/Release/badge.svg?branch=master)](https://github.com/Jaitl/cloud-crawler/actions?workflow=Release)
[![Version](https://img.shields.io/github/release/Jaitl/cloud-crawler.svg?label=Version)](https://github.com/Jaitl/cloud-crawler/releases)
[![Coverage Status](https://coveralls.io/repos/github/Jaitl/cloud-crawler/badge.svg?branch=master)](https://coveralls.io/github/Jaitl/cloud-crawler?branch=master)
# cloudCrawler
A framework for building a distributed, highload system for crawling open data.

## Run
1. Run master node:
    ```
    sbt master/run
    ```
2. Run worker nodes:
    ```
    sbt simple-worker/run
    sbt simple-worker/run
    ```

## MongoDB data example
1. СonfigurationСollection
    ```
    {
        "workerExecuteInterval" : "35.seconds",
        "workerFilePath" : "/",
        "workerBatchSize" : 2.0,
        "workerBaseUrl" : "https://habr.com/ru",
        "workerTaskType" : "HabrTasks",
        "workerParallelBatches" : 1,
        "workerResource" : "Tor",
        "workerNotification" : false
    }
    ```
2. TorCollection
    ```
    {
        "workerTorHost" : "127.0.0.1",
        "workerTorLimit" : 1,
        "workerTorPort" : 9150,
        "workerTorControlPort" : 0,
        "workerTorPassword" : "",
        "workerTorTimeoutUp" : "30.seconds",
        "workerTorTimeoutDown" : "30.seconds",
        "workerTaskType" : [ 
            "HabrTasks"
        ],
        "usedCount" : 0
    }
    ```
3. CrawlTasks
    ```
    {
        "taskType" : "HabrTasks",
        "taskData" : "438886",
        "taskStatus" : "taskWait",
        "attempt" : 0
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
