package com.github.jaitl.crawler.base.common.request

import java.util.UUID

case class NoTasks(requestId: UUID, taskType: String) extends TaskRequest
