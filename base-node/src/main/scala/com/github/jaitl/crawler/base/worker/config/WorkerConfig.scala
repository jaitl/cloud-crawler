package com.github.jaitl.crawler.base.worker.config

import scala.concurrent.duration.FiniteDuration

case class WorkerConfig(parallelBatches: Int, executeInterval: FiniteDuration)
