package com.github.jaitl.cloud.common.models.result

import java.util.UUID

case class TasksBatchProcessResult(
  requestId: UUID,
  taskType: String,
  successIds: Seq[String],
  failureIds: Seq[String],
  newTasks: Map[String, Seq[String]]
)
