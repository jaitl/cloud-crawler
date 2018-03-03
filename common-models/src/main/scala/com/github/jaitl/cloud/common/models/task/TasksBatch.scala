package com.github.jaitl.cloud.common.models.task

import java.util.UUID

case class TasksBatch(id: UUID, taskType: String, tasks: Seq[Task])
