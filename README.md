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