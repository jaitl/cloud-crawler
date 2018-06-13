package com.github.jaitl.crawler.models.task

import java.util.UUID

case class TasksBatch(id: UUID, taskType: String, tasks: Seq[Task])
