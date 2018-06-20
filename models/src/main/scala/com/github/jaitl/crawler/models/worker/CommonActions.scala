package com.github.jaitl.crawler.models.worker

import java.util.UUID

object CommonActions {
  case class ActionSuccess(requestId: UUID, taskType: String)
}
