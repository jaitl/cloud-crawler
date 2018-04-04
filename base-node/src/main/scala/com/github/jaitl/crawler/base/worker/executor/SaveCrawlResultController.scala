package com.github.jaitl.crawler.base.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Stash
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.TasksBatchProcessResult
import com.github.jaitl.crawler.base.models.task.Task
import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.AutoSaveResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.base.worker.parser.ParseResult
import com.github.jaitl.crawler.base.worker.parser.ParsedData
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline
import com.github.jaitl.crawler.base.worker.scheduler.Scheduler

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class SaveCrawlResultController(
  pipeline: Pipeline[_ <: ParsedData],
  queueTaskBalancer: ActorRef,
  tasksBatchController: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends Actor with ActorLogging with Stash {
  private var successTasks: mutable.Seq[SuccessCrawledTask] = mutable.ArraySeq.empty[SuccessCrawledTask]
  private var failedTasks: mutable.Seq[FailedTask] = mutable.ArraySeq.empty[FailedTask]

  override def preStart(): Unit = {
    super.preStart()

    saveScheduler.schedule(config.saveInterval, self, SaveResults)
  }

  override def receive: Receive = addResultHandler orElse waitSave

  private def addResultHandler: Receive = {
    case AddResults(result) =>
      result match {
        case a @ SuccessCrawledTask(_, _, _) =>
          successTasks :+ a
          sender() ! SuccessAddedResults

        case a @ FailedTask(_, _) =>
          failedTasks :+ a
          sender() ! SuccessAddedResults
      }
  }

  private def waitSave: Receive = {
    case AutoSaveResults =>
      context.become(saveResultHandler)
    case SaveResults =>
      context.become(saveResultHandler)
  }

  private def saveResultHandler: Receive = {
    case success @ SuccessSavedResults =>
      context.unbecome()
      unstashAll()

      val successIds = successTasks.map(_.task.id)
      val failureIds = failedTasks.map(_.task.id)
      val newCrawlTasks = successTasks.flatMap(_.parseResult.map(_.newCrawlTasks).getOrElse(Seq.empty))
      val newTasks = newCrawlTasks.groupBy(_.taskType)
        .map {
          case (taskType, vals) =>
            val newTasks = vals.flatMap(_.tasks).distinct
            (taskType, newTasks)
        }

      queueTaskBalancer ! TasksBatchProcessResult(
        requestId = UUID.randomUUID(),
        taskType = pipeline.taskType,
        successIds = successIds,
        failureIds = failureIds,
        newTasks = newTasks
      )

      successTasks = mutable.ArraySeq.empty[SuccessCrawledTask]
      failedTasks = mutable.ArraySeq.empty[FailedTask]

      tasksBatchController ! success

    case failure @ FailureSaveResults(t) =>
      log.error(t, "Error during save results")
      context.unbecome()
      unstashAll()

      tasksBatchController ! failure

    case _: Any => stash()
  }
}

object SaveCrawlResultController {

  trait SaveResults
  case object AutoSaveResults extends SaveResults
  case object SaveResults extends SaveResults
  case object SuccessSavedResults extends SaveResults
  case class FailureSaveResults(t: Throwable) extends SaveResults

  case class AddResults(result: CrawlTaskResult)
  case object SuccessAddedResults

  trait CrawlTaskResult
  case class SuccessCrawledTask(task: Task, crawlResult: CrawlResult, parseResult: Option[ParseResult[_ <: ParsedData]]) extends CrawlTaskResult
  case class FailedTask(task: Task, t: Seq[Throwable]) extends CrawlTaskResult

  case class SaveCrawlResultControllerConfig(saveInterval: FiniteDuration)

  def props(
    pipeline: Pipeline[_ <: ParsedData],
    queueTaskBalancer: ActorRef,
    tasksBatchController: ActorRef,
    saveScheduler: Scheduler,
    config: SaveCrawlResultControllerConfig
  ): Props = Props(new SaveCrawlResultController(
    pipeline = pipeline,
    queueTaskBalancer = queueTaskBalancer,
    tasksBatchController = tasksBatchController,
    saveScheduler = saveScheduler,
    config = config
  ))

  def name(): String = "saveCrawlResultController"
}

class SaveCrawlResultControllerCreator(
  queueTaskBalancer: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends TwoArgumentActorCreator[Pipeline[_ <: ParsedData], ActorRef] {
  override def create(factory: ActorRefFactory, firstArg: Pipeline[_ <: ParsedData], secondArg: ActorRef): ActorRef = {
    factory.actorOf(
      props = SaveCrawlResultController.props(
        pipeline = firstArg,
        queueTaskBalancer = queueTaskBalancer,
        tasksBatchController = secondArg,
        saveScheduler = saveScheduler,
        config = config
      ),
      name = SaveCrawlResultController.name()
    )
  }
}
