package com.github.jaitl.crawler.models.worker

import java.util.UUID

import com.github.jaitl.crawler.models.task.TasksBatch

object WorkerManager {
  case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize])

  case class RequestConfiguration(requestId: UUID, taskType: String)

  case class RequestProxy(requestId: UUID, taskType: String)

  case class RequestTor(requestId: UUID, taskType: String)

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

  case class SuccessTasksConfigRequest(requestId: UUID, taskType: String, head: ProjectConfiguration)

  case class FailureConfigRequest(requestId: UUID, taskType: String, throwable: Throwable)

  case class NoConfigs(requestId: UUID, taskType: String)

  case class SuccessProxyRequest(requestId: UUID, taskType: String, head: CrawlerProxy)

  case class FailureProxyRequest(requestId: UUID, taskType: String, throwable: Throwable)

  case class NoProxies(requestId: UUID, taskType: String)

  case class SuccessTorRequest(requestId: UUID, taskType: String, head: CrawlerTor)

  case class FailureTorRequest(requestId: UUID, taskType: String, throwable: Throwable)

  case class NoTors(requestId: UUID, taskType: String)

  case object EmptyTaskTypeList
}
