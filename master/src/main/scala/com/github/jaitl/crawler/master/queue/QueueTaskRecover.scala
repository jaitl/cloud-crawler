package com.github.jaitl.crawler.master.queue

import java.time.Instant

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import com.github.jaitl.crawler.master.queue.QueueTaskRecover.Recover
import com.github.jaitl.crawler.master.queue.QueueTaskRecover.RecoveryConfig
import com.github.jaitl.crawler.master.queue.QueueTaskRecover.Stop
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.master.scheduler.Scheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success

class QueueTaskRecover(
  queueTaskProvider: QueueTaskProvider,
  recoverScheduler: Scheduler,
  config: RecoveryConfig
) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  recoverScheduler.schedule(config.recoveryCheckPeriod, self, Recover)

  override def receive: Receive = {
    case Recover =>
      val now = Instant.now()

      val recoveryResult = queueTaskProvider.updateTasksStatusFromTo(
        now.minusMillis(config.recoveryTimeout.toMillis), TaskStatus.taskInProgress, TaskStatus.taskWait
      )

      recoveryResult.onComplete {
        case Success(recoveryCount) =>
          if (recoveryCount > 0) {
            log.warning(s"Recovered tasks: $recoveryCount")
          }

        case Failure(ex) =>
          log.error(ex, "Error during tasks recovery")
      }

    case Stop =>
      context.stop(self)
  }
}

object QueueTaskRecover {
  case object Recover
  case object Stop

  case class RecoveryConfig(recoveryTimeout: FiniteDuration, recoveryCheckPeriod: FiniteDuration)

  def props(
    queueTaskProvider: QueueTaskProvider,
    recoverScheduler: Scheduler,
    config: RecoveryConfig
  ): Props = Props(new QueueTaskRecover(queueTaskProvider, recoverScheduler, config))

  def name(): String = "QueueTaskRecover"
}
