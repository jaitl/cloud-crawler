package com.github.jaitl.crawler.worker.crawler

case class CrawlTask(
  taskId: String,
  taskData: String,
  taskType: String,
  projectId: String,
  nextProjectId: String,
  baseDomain: String)
