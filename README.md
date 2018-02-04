[![Build Status](https://travis-ci.org/Jaitl/cloud-crawler.svg?branch=master)](https://travis-ci.org/Jaitl/cloud-crawler)
[![Coverage Status](https://coveralls.io/repos/github/Jaitl/cloud-crawler/badge.svg?branch=master)](https://coveralls.io/github/Jaitl/cloud-crawler?branch=master)
# cloudCrawler
Distributed highly loaded system for data crawling.

# Run
1. Run seed nodes:
    ```
    ./gradlew :seed:run -DSEED_PORT=2551
    ./gradlew :seed:run -DSEED_PORT=2552
    ```
2. Run master node:
    ```
    ./gradlew :master:run -DMASTER_PORT=2561
    ```
3. Run worker nodes:
    ```
    ./gradlew :simple-worker:run -DWORKER_PORT=2571
    ./gradlew :simple-worker:run -DWORKER_PORT=2572
    ```