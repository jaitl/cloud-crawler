package com.github.jaitl.crawler.base.common.request

import java.util.UUID

case class FailureTasksBatchRequest(
  requestId: UUID,
  taskType: String,
  throwable: Throwable
) extends TaskRequest
