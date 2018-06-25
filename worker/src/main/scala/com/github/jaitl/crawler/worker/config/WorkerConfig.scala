package com.github.jaitl.crawler.worker.config

import scala.concurrent.duration.FiniteDuration

private[worker] case class WorkerConfig(
  parallelBatches: Int,
  executeInterval: FiniteDuration,
  runExecutionTimeoutCheckInterval: FiniteDuration,
  batchExecutionTimeout: FiniteDuration
)
