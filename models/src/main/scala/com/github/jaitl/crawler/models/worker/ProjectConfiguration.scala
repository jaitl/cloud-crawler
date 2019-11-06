package com.github.jaitl.crawler.models.worker

case class ProjectConfiguration(
  _id: String,
  workerExecuteInterval: String,
  workerFilePath: String,
  workerBatchSize: Int,
  workerBaseUrl: String,
  workerTaskType: String,
  workerParallelBatches: Int,
  workerResource: String
)
