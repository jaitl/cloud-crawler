package com.github.jaitl.crawler.models.worker

case class CrawlerTor(
  id: String,
  workerTorHost: String,
  workerTorLimit: Int,
  workerTorPort: Int,
  workerTorControlPort: Int,
  workerTorPassword: String,
  workerTorTimeoutUp: String,
  workerTorTimeoutDown: String,
  workerTaskType: String
)