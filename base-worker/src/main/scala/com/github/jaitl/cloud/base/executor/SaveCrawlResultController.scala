package com.github.jaitl.cloud.base.executor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Stash
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveFailed
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveSuccess
import com.github.jaitl.cloud.base.executor.TasksBatchController.FailedTask
import com.github.jaitl.cloud.base.executor.TasksBatchController.SuccessCrawledTask
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.base.scheduler.Scheduler

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

  override def preStart(): Unit = {
    super.preStart()

    saveScheduler.schedule(config.saveInterval, self, SaveResults)
  }

  override def receive: Receive = ???

  private def waitSave: Receive = {
    case SaveResults =>
      context.become(saveResultHandler)
  }

  private def saveResultHandler: Receive = {
    case SaveSuccess =>
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

    case SaveFailed(t) =>
      log.error(t, "Error during save results")
      context.unbecome()
      unstashAll()

    case _: Any => stash()
  }
}

object SaveCrawlResultController {
  case object SaveResults
  case object SaveSuccess
  case class SaveFailed(t: Throwable)

  case class SaveCrawlResultControllerConfig(saveInterval: FiniteDuration)
}
