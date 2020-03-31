package com.github.jaitl.crawler.master.queue

import scala.concurrent.duration.Duration

case class QueueTaskConfig(
  maxAttemptsCount: Int,
  dbRequestTimeout: Duration
)
