package com.github.jaitl.crawler.worker.client

import java.util.UUID

import com.github.jaitl.crawler.master.client.task.TaskTypeWithBatchSize
import com.github.jaitl.crawler.master.client.task.TasksBatch

import scala.concurrent.Future

trait QueueClient {
  def getTasks(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize]): Future[Option[TasksBatch]]
  def returnTasks(requestId: UUID, ids: Seq[String]): Future[Unit]
  def putProcessResult(
    requestId: UUID,
    successIds: Seq[String],
    failureIds: Seq[String],
    skippedIds: Seq[String],
    parsingFailedTaskIds: Seq[String],
    bannedIds: Seq[String],
    newTasks: Map[String, Seq[String]]
  ): Future[Unit]
}
