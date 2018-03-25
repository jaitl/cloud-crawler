package com.github.jaitl.crawler.base.common.request

import java.util.UUID

case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize]) extends TaskRequest

case class TaskTypeWithBatchSize(taskType: String, batchSize: Int)
