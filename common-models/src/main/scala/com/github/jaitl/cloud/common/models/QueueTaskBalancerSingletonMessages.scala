package com.github.jaitl.cloud.common.models

import java.util.UUID

object QueueTaskBalancerSingletonMessages {
  case class GetWork(requestId: UUID, taskTypes: Seq[String])
  case object Stop
}
