package com.github.jaitl.cloud.common.models.request

import java.util.UUID

import com.github.jaitl.cloud.common.models.task.TasksBatch

case class SuccessTasksBatchRequest(
  requestId: UUID,
  taskType: String,
  tasksBatch: TasksBatch
) extends TaskRequest
