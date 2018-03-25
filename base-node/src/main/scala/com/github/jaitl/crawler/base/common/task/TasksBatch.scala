package com.github.jaitl.crawler.base.common.task

import java.util.UUID

case class TasksBatch(id: UUID, taskType: String, tasks: Seq[Task])
