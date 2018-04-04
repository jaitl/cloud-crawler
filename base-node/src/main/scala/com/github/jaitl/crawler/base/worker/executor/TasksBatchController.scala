package com.github.jaitl.crawler.base.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Stash
import com.github.jaitl.crawler.base.models.task.Task
import com.github.jaitl.crawler.base.models.task.TasksBatch
import com.github.jaitl.crawler.base.worker.creator.ActorCreator
import com.github.jaitl.crawler.base.worker.creator.OneArgumentActorCreator
import com.github.jaitl.crawler.base.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.Crawl
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController.ExecuteTask
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController.QueuedTask
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.NoFreeResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.RequestResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceHepler
import com.github.jaitl.crawler.base.worker.parser.ParsedData
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline
import com.github.jaitl.crawler.base.worker.pipeline.ResourceType
import com.github.jaitl.crawler.base.worker.scheduler.Scheduler

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

private class TasksBatchController(
  batch: TasksBatch,
  pipeline: Pipeline[ParsedData],
  resourceControllerCreator: OneArgumentActorCreator[ResourceType],
  crawlExecutorCreator: ActorCreator,
  saveCrawlResultCreator: TwoArgumentActorCreator[Pipeline[ParsedData], ActorRef],
  executeScheduler: Scheduler,
  config: TasksBatchControllerConfig
) extends Actor with ActorLogging with Stash {
  private var currentActiveCrawlTask: Int = 0
  private var forcedStop: Boolean = false

  private val taskQueue: mutable.Queue[QueuedTask] = mutable.Queue.apply(batch.tasks.map(QueuedTask(_, 0)): _*)

  private var resourceController: ActorRef = _
  private var saveCrawlResultController: ActorRef = _
  private var crawlExecutor: ActorRef = _

  override def preStart(): Unit = {
    super.preStart()

    resourceController = resourceControllerCreator.create(this.context, pipeline.resourceType)
    saveCrawlResultController = saveCrawlResultCreator.create(this.context, pipeline, self)
    crawlExecutor = crawlExecutorCreator.create(this.context)

    executeScheduler.schedule(config.executeInterval, self, ExecuteTask)
  }

  override def receive: Receive = Seq(waitRequest, resourceRequestHandler, crawlResultHandler, resultSavedHandler)
    .reduce(_ orElse _)

  private def waitRequest: Receive = {
    case ExecuteTask =>
      if (taskQueue.nonEmpty || !forcedStop) {
        resourceController ! RequestResource(UUID.randomUUID(), batch.taskType)
      } else {
        saveCrawlResultController ! SaveResults
      }
  }

  private def resourceRequestHandler: Receive = {
    case SuccessRequestResource(requestId, requestExecutor) =>
      if (taskQueue.nonEmpty) {
        val task = taskQueue.dequeue()
        crawlExecutor ! Crawl(requestId, task, requestExecutor, pipeline)
        self ! ExecuteTask
        currentActiveCrawlTask = currentActiveCrawlTask + 1
      } else {
        resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)
      }

    case NoFreeResource(requestId) =>
      log.debug(s"NoFreeResource, requestId: $requestId")

    case NoResourcesAvailable(requestId) =>
      log.debug(s"NoResourcesAvailable, requestId: $requestId")
      forcedStop = true
      resourceController ! SaveResults
  }

  private def crawlResultHandler: Receive = {
    case CrawlSuccessResult(requestId, task, requestExecutor, crawlResult, parseResult) =>
      resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)
      saveCrawlResultController ! AddResults(SuccessCrawledTask(task.task, crawlResult, parseResult))

    case CrawlFailureResult(requestId, task, requestExecutor, t) =>
      if (ResourceHepler.isResourceFailed(t)) {
        resourceController ! ReturnFailedResource(requestId, batch.taskType, requestExecutor, t)
        taskQueue += task
        currentActiveCrawlTask = currentActiveCrawlTask - 1
      } else {
        resourceController ! ReturnSuccessResource(requestId, batch.taskType, requestExecutor)

        if (task.attempt < config.maxAttempts) {
          taskQueue += task.copy(attempt = task.attempt + 1, t = task.t :+ t)
          currentActiveCrawlTask = currentActiveCrawlTask - 1
        } else {
          saveCrawlResultController ! AddResults(FailedTask(task.task, task.t :+ t))
        }
      }
  }

  private def resultSavedHandler: Receive = {
    case SuccessAddedResults =>
      currentActiveCrawlTask = currentActiveCrawlTask - 1

    case SuccessSavedResults =>
      if ((taskQueue.isEmpty || forcedStop) && currentActiveCrawlTask == 0) {
        log.info(s"Stop task batch controller: ${batch.id}, forcedStop: $forcedStop")
        context.stop(self)
      }

    case FailureSaveResults(t) =>
      log.error(t, "Error during save results")
  }
}

private[base] object TasksBatchController {

  case object ExecuteTask

  case class QueuedTask(task: Task, attempt: Int, t: Seq[Throwable] = Seq.empty)

  def props(
    batch: TasksBatch,
    pipeline: Pipeline[ParsedData],
    resourceControllerCreator: OneArgumentActorCreator[ResourceType],
    crawlExecutorCreator: ActorCreator,
    saveCrawlResultCreator: TwoArgumentActorCreator[Pipeline[ParsedData], ActorRef],
    executeScheduler: Scheduler,
    config: TasksBatchControllerConfig
  ): Props =
    Props(new TasksBatchController(
      batch = batch,
      pipeline = pipeline,
      resourceControllerCreator = resourceControllerCreator,
      crawlExecutorCreator = crawlExecutorCreator,
      saveCrawlResultCreator = saveCrawlResultCreator,
      executeScheduler = executeScheduler,
      config = config
    ))

  def name(batchId: UUID): String = s"tasksBatchController-$batchId"

  case class TasksBatchControllerConfig(
    maxAttempts: Int,
    executeInterval: FiniteDuration
  )
}

class TasksBatchControllerCreator(
  resourceControllerCreator: OneArgumentActorCreator[ResourceType],
  crawlExecutorCreator: ActorCreator,
  saveCrawlResultCreator: TwoArgumentActorCreator[Pipeline[ParsedData], ActorRef],
  executeScheduler: Scheduler,
  config: TasksBatchControllerConfig
) extends TwoArgumentActorCreator[TasksBatch, Pipeline[ParsedData]] {
  override def create(
    factory: ActorRefFactory, firstArg: TasksBatch, secondArg: Pipeline[ParsedData]
  ): ActorRef = {
    factory.actorOf(
      props = TasksBatchController.props(
        batch = firstArg,
        pipeline = secondArg,
        resourceControllerCreator = resourceControllerCreator,
        crawlExecutorCreator = crawlExecutorCreator,
        saveCrawlResultCreator = saveCrawlResultCreator,
        executeScheduler = executeScheduler,
        config = config
      ),
      name = TasksBatchController.name(firstArg.id)
    )
  }
}
