package com.github.jaitl.cloud.base.executor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Stash
import com.github.jaitl.cloud.base.crawler.CrawlResult
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.AutoSaveResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.cloud.base.parser.ParseResult
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.base.scheduler.Scheduler
import com.github.jaitl.cloud.common.models.task.Task

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

class SaveCrawlResultController(
  pipeline: Pipeline,
  queueTaskBalancer: ActorRef,
  saveScheduler: Scheduler,
  config: SaveCrawlResultControllerConfig
) extends Actor with ActorLogging with Stash {
  private var totalCrawledCount = 0
  private var saveAttemptsCount: Int = 0

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

    case SaveResults =>
      context.become(saveResultHandler)
  }

  private def saveResultHandler: Receive = {
    case SuccessSavedResults =>
      context.unbecome()
      unstashAll()

      // TODO send results to queueTaskBalancer
      successTasks = mutable.ArraySeq.empty[SuccessCrawledTask]
      failedTasks = mutable.ArraySeq.empty[FailedTask]

      // TODO wait until all crawl task is completed
      if (taskQueue.isEmpty) {
        if (saveAttemptsCount > config.finalMaxSaveAttempts || batch.tasks.length == totalCrawledCount) {
          context.stop(self)
        } else {
          saveAttemptsCount += saveAttemptsCount + 1
        }
      } else {
        saveAttemptsCount = 0
      }

    case FailureSaveResults(t) =>
      log.error(t, "Error during save results")
      context.unbecome()
      unstashAll()

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
  case class SuccessCrawledTask(task: Task, crawlResult: CrawlResult, parseResult: Option[ParseResult]) extends CrawlTaskResult
  case class FailedTask(task: Task, t: Seq[Throwable]) extends CrawlTaskResult

  case class SaveCrawlResultControllerConfig(saveInterval: FiniteDuration)
}
