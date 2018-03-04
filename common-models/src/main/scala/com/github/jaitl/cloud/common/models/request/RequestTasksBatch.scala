package com.github.jaitl.cloud.common.models.request

import java.util.UUID

case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[String], batchSize: Int) extends TaskRequest
