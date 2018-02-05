package com.github.jaitl.cloud.base

import java.util.UUID

object QueueTaskBalancerSingletonMessages {
  case class GetWork(requestId: UUID, taskTypes: Seq[String])
  case object Stop
}
