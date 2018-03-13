package com.github.jaitl.cloud.base.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Stash
import com.github.jaitl.cloud.base.crawler.CrawlResult
import com.github.jaitl.cloud.base.creator.ActorCreator
import com.github.jaitl.cloud.base.creator.OneArgumentActorCreator
import com.github.jaitl.cloud.base.creator.TwoArgumentActorCreator
import com.github.jaitl.cloud.base.executor.CrawlExecutor.Crawl
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.cloud.base.executor.TasksBatchController.ExecuteTask
import com.github.jaitl.cloud.base.executor.TasksBatchController.FailedTask
import com.github.jaitl.cloud.base.executor.TasksBatchController.QueuedTask
import com.github.jaitl.cloud.base.executor.TasksBatchController.SaveFailed
import com.github.jaitl.cloud.base.executor.TasksBatchController.SaveResults
import com.github.jaitl.cloud.base.executor.TasksBatchController.SaveSuccess
import com.github.jaitl.cloud.base.executor.TasksBatchController.SuccessCrawledTask
import com.github.jaitl.cloud.base.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.cloud.base.executor.resource.ResourceController.NoFreeResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.cloud.base.executor.resource.ResourceController.RequestResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.cloud.base.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.cloud.base.executor.resource.ResourceHepler
import com.github.jaitl.cloud.base.parser.ParseResult
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.base.pipeline.ResourceType
import com.github.jaitl.cloud.base.scheduler.Scheduler
import com.github.jaitl.cloud.common.models.task.Task
import com.github.jaitl.cloud.common.models.task.TasksBatch

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

private class TasksBatchController(
  batch: TasksBatch,
  pipeline: Pipeline,
  resourceControllerCreator: OneArgumentActorCreator[ResourceType],
  crawlExecutorCreator: ActorCreator,
  executeScheduler: Scheduler,
  config: TasksBatchControllerConfig
) extends Actor with ActorLogging with Stash {
  private var currentActiveCrawlTask: Int = 0

  private val taskQueue: mutable.Queue[QueuedTask] = mutable.Queue.apply(batch.tasks.map(QueuedTask(_, 0)): _*)

  private var resourceController: ActorRef = _
  private var crawlExecutor: ActorRef = _

  private var successTasks: mutable.Seq[SuccessCrawledTask] = mutable.ArraySeq.empty[SuccessCrawledTask]
  private var failedTasks: mutable.Seq[FailedTask] = mutable.ArraySeq.empty[FailedTask]

  override def preStart(): Unit = {
    super.preStart()

    resourceController = resourceControllerCreator.create(this.context, pipeline.resourceType)
    crawlExecutor = crawlExecutorCreator.create(this.context)

    executeScheduler.schedule(config.executeInterval, self, ExecuteTask)
  }

  override def receive: Receive = Seq(waitRequest, waitSave, resourceRequestHandler, crawlResultHandler)
    .reduce(_ orElse _)

  private def waitRequest: Receive = {
    case ExecuteTask =>
      if (taskQueue.nonEmpty) {
        resourceController ! RequestResource(UUID.randomUUID(), batch.taskType)
      } else {
        // TODO save if complete
        self ! SaveResults(forced = false)
      }
  }

  private def resourceRequestHandler: Receive = {
    case SuccessRequestResource(requestId, requestExecutor) =>
      if (taskQueue.nonEmpty) {
        val task = taskQueue.dequeue()
        crawlExecutor ! Crawl(requestId, task, requestExecutor, pipeline)

        self ! ExecuteTask
      } else {
        resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)
      }

    case NoFreeResource(requestId) =>
      log.debug(s"NoFreeResource, requestId: $requestId")

    case NoResourcesAvailable(requestId) =>
      log.debug(s"NoResourcesAvailable, requestId: $requestId")

      sender() ! SaveResults(forced = true)
  }

  private def crawlResultHandler: Receive = {
    case CrawlSuccessResult(requestId, task, requestExecutor, crawlResult, parseResult) =>
      resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)
      successTasks :+ SuccessCrawledTask(task.task, crawlResult, parseResult)
      totalCrawledCount = totalCrawledCount + 1

    case CrawlFailureResult(requestId, task, requestExecutor, t) =>
      if (ResourceHepler.isResourceFailed(t)) {
        resourceController ! ReturnFailedResource(requestId, batch.taskType, requestExecutor, t)
        taskQueue += task
      } else {
        resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)

        if (task.attempt < config.maxAttempts) {
          taskQueue += task.copy(attempt = task.attempt + 1, t = task.t :+ t)
        } else {
          failedTasks :+ FailedTask(task.task, task.t :+ t)
          totalCrawledCount = totalCrawledCount + 1
        }
      }
  }

}

private[base] object TasksBatchController {

  case object ExecuteTask

  case class QueuedTask(task: Task, attempt: Int, t: Seq[Throwable] = Seq.empty)

  case class SuccessCrawledTask(task: Task, crawlResult: CrawlResult, parseResult: Option[ParseResult])

  case class FailedTask(task: Task, t: Seq[Throwable])

  def props(
    batch: TasksBatch,
    pipeline: Pipeline,
    resourceControllerCreator: OneArgumentActorCreator[ResourceType],
    crawlExecutorCreator: ActorCreator
  ): Props =
    Props(new TasksBatchController(batch, pipeline, resourceControllerCreator, crawlExecutorCreator))

  def name(batchId: UUID): String = s"tasksBatchController-$batchId"

  case class TasksBatchControllerConfig(
    maxAttempts: Int,
    finalMaxSaveAttempts: Int,
    saveInterval: FiniteDuration,
    executeInterval: FiniteDuration
  )
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
