package com.github.jaitl.cloud.base.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.base.executor.CrawlExecutor.Crawl
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.cloud.base.executor.TasksBatchController.ExecuteTask
import com.github.jaitl.cloud.base.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.cloud.base.executor.resource.ResourceController.RequestResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.ReturnResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.task.Task
import com.github.jaitl.cloud.common.models.task.TasksBatch

import scala.collection.mutable

private class TasksBatchController(
  batch: TasksBatch,
  crawlExecutorRouter: ActorRef,
  pipeline: Pipeline
) extends Actor with ActorLogging {
  private val taskQueue: mutable.Queue[Task] = mutable.Queue.apply(batch.tasks: _*)

  private var resourceController: ActorRef = ???

  override def receive: Receive = Seq(waitRequest, resourceRequestHandler, crawlResultHandler).reduce(_ orElse _)

  private def waitRequest: Receive = {
    case ExecuteTask =>
      if (taskQueue.nonEmpty) {
        resourceController ! RequestResource(UUID.randomUUID(), batch.taskType)
      }
  }

  private def resourceRequestHandler: Receive = {
    case SuccessRequestResource(requestId, resource) =>
      if (taskQueue.nonEmpty) {
        val task = taskQueue.dequeue()
        crawlExecutorRouter ! Crawl(requestId, task, resource, pipeline)
      } else {
        resourceController ! ReturnResource(requestId, batch.taskType, resource)
        scheduleTimeout()
      }

    case NoResourcesAvailable(requestId) =>
      scheduleTimeout()
  }

  private def crawlResultHandler: Receive = {
    case CrawlSuccessResult(requestId, task, resource, crawlResult, parseResult) =>
      resourceController ! ReturnResource(requestId, batch.taskType, resource)

    case CrawlFailureResult(requestId, task, resource, throwable) =>
      resourceController ! ReturnResource(requestId, batch.taskType, resource)

  }

  private def scheduleTimeout(): Unit = ???
}

private[base] object TasksBatchController {

  case object ExecuteTask

  def props(batch: TasksBatch, crawlExecutorRouter: ActorRef, pipeline: Pipeline): Props =
    Props(new TasksBatchController(batch, crawlExecutorRouter, pipeline))

  def name(batchId: UUID): String = s"tasksBatchController-$batchId"
}
