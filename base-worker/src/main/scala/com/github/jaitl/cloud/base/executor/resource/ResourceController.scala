package com.github.jaitl.cloud.base.executor.resource

import java.util.UUID

import com.github.jaitl.cloud.common.models.resource.Resource

private[executor] object ResourceController {
  case class RequestResource(requestId: UUID, taskType: String)
  case class SuccessRequestResource(requestId: UUID, resource: Resource)
  case class NoResourcesAvailable(requestId: UUID)
  case class ReturnResource(requestId: UUID, taskType: String, resource: Resource)
}
