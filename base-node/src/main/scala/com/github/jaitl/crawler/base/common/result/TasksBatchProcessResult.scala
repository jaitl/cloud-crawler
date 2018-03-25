package com.github.jaitl.crawler.base.common.result

import java.util.UUID

case class TasksBatchProcessResult(
  requestId: UUID,
  taskType: String,
  successIds: Seq[String],
  failureIds: Seq[String],
  newTasks: Map[String, Seq[String]]
)
