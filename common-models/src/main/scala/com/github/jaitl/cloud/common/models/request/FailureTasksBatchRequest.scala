package com.github.jaitl.cloud.common.models.request

import java.util.UUID

case class FailureTasksBatchRequest(
  requestId: UUID,
  taskType: String,
  throwable: Throwable
) extends TaskRequest
