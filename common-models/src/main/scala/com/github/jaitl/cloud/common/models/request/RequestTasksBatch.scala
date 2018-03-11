package com.github.jaitl.cloud.common.models.request

import java.util.UUID

case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize]) extends TaskRequest

case class TaskTypeWithBatchSize(taskType: String, batchSize: Int)
