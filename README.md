[![Build Status](https://travis-ci.org/Jaitl/cloud-crawler.svg?branch=master)](https://travis-ci.org/Jaitl/cloud-crawler)
[![Coverage Status](https://coveralls.io/repos/github/Jaitl/cloud-crawler/badge.svg?branch=master)](https://coveralls.io/github/Jaitl/cloud-crawler?branch=master)
# cloudCrawler
A framework for building a distributed, highload system for crawling open data.

# Running a single node with master and worker
```
./gradlew :simple-worker:run
```

# Running multiple nodes with master and worker together
```
./gradlew :simple-worker:run -DNODE_ROLES=master,worker -DNODE_PORT=2551 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
./gradlew :simple-worker:run -DNODE_ROLES=master,worker -DNODE_PORT=2552 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
```

# Running multiple nodes with master and worker separately
1. Run multiple master
    ```
    ./gradlew :simple-worker:run -DNODE_ROLES=master -DNODE_PORT=2551 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
    ./gradlew :simple-worker:run -DNODE_ROLES=master -DNODE_PORT=2552 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
    ```
2. Run multiple worker
    ```
    ./gradlew :simple-worker:run -DNODE_ROLES=worker -DNODE_PORT=2561 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
    ./gradlew :simple-worker:run -DNODE_ROLES=worker -DNODE_PORT=2562 -DCLUSTER_SEEDS=127.0.0.1:2551,127.0.0.1:2552
    ```
