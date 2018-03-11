package com.github.jaitl.cloud.base.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import com.github.jaitl.cloud.base.creator.ActorCreator
import com.github.jaitl.cloud.base.creator.OneArgumentActorCreator
import com.github.jaitl.cloud.base.creator.TwoArgumentActorCreator
import com.github.jaitl.cloud.base.executor.CrawlExecutor.Crawl
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.cloud.base.executor.TasksBatchController.ExecuteTask
import com.github.jaitl.cloud.base.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.cloud.base.executor.resource.ResourceController.RequestResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.ReturnResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.base.pipeline.ResourceType
import com.github.jaitl.cloud.common.models.task.Task
import com.github.jaitl.cloud.common.models.task.TasksBatch

import scala.collection.mutable

private class TasksBatchController(
  batch: TasksBatch,
  pipeline: Pipeline,
  resourceControllerCreator: OneArgumentActorCreator[ResourceType],
  crawlExecutorCreator: ActorCreator
) extends Actor with ActorLogging {
  private val taskQueue: mutable.Queue[Task] = mutable.Queue.apply(batch.tasks: _*)

  private var resourceController: ActorRef = _
  private var crawlExecutor: ActorRef = _

  override def preStart(): Unit = {
    super.preStart()

    resourceController = resourceControllerCreator.create(this.context, pipeline.resourceType)
    crawlExecutor = crawlExecutorCreator.create(this.context)
  }

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
        crawlExecutor ! Crawl(requestId, task, resource, pipeline)
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

  def props(
    batch: TasksBatch,
    pipeline: Pipeline,
    resourceControllerCreator: OneArgumentActorCreator[ResourceType],
    crawlExecutorCreator: ActorCreator
  ): Props =
    Props(new TasksBatchController(batch, pipeline, resourceControllerCreator, crawlExecutorCreator))

  def name(batchId: UUID): String = s"tasksBatchController-$batchId"
}

class TasksBatchControllerCreator(
  resourceControllerCreator: OneArgumentActorCreator[ResourceType],
  crawlExecutorCreator: ActorCreator
) extends TwoArgumentActorCreator[TasksBatch, Pipeline] {
  override def create(
    factory: ActorRefFactory, firstArg: TasksBatch, secondArg: Pipeline
  ): ActorRef = {
    factory.actorOf(
      props = TasksBatchController.props(firstArg, secondArg, resourceControllerCreator, crawlExecutorCreator),
      name = TasksBatchController.name(firstArg.id)
    )
  }
}
