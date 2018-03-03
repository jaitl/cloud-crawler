package com.github.jaitl.cloud.common.models.request

import java.util.UUID

case class NoTasks(requestId: UUID, taskType: String) extends TaskRequest
