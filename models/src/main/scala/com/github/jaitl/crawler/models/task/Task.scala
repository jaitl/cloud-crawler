package com.github.jaitl.crawler.models.task

import java.time.Instant

case class Task(
  id: String,
  taskType: String,
  taskData: String,
  attempt: Int = 0,
  lastUpdate: Option[Instant] = None,
  skipped: Boolean
)
