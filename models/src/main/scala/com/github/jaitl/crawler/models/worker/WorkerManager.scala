package com.github.jaitl.crawler.models.worker

import java.util.UUID

import com.github.jaitl.crawler.models.task.TasksBatch

object WorkerManager {
  case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize])

  case class  TasksBatchProcessResult(
    requestId: UUID,
    taskType: String,
    successIds: Seq[String],
    failureIds: Seq[String],
    skippedIds: Seq[String],
    bannedIds: Seq[String],
    newTasks: Map[String, Seq[String]]
  )

  case class ReturnTasks(requestId: UUID, taskType: String, ids: Seq[String])

  case class TaskTypeWithBatchSize(taskType: String, batchSize: Int)

  case object RequestBatch

  case class SuccessTasksBatchRequest(
    requestId: UUID,
    taskType: String,
    tasksBatch: TasksBatch
  )

  case class FailureTasksBatchRequest(
    requestId: UUID,
    taskType: String,
    throwable: Throwable
  )

  case class NoTasks(requestId: UUID, taskType: String)

  case object EmptyTaskTypeList
}
