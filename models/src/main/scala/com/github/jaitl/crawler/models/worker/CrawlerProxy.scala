package com.github.jaitl.crawler.models.worker

case class CrawlerProxy(
  id: String,
  workerProxyHost: String,
  workerProxyPort: Int,
  workerProxyTimeoutUp: String,
  workerProxyTimeoutDown: String,
  workerParallel: Int,
  workerProxyLogin: String,
  workerProxyPassword: String,
  workerTaskType: String
)
