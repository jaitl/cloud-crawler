package com.github.jaitl.crawler.base.common.request

import java.util.UUID

import com.github.jaitl.crawler.base.common.task.TasksBatch

case class SuccessTasksBatchRequest(
  requestId: UUID,
  taskType: String,
  tasksBatch: TasksBatch
) extends TaskRequest
