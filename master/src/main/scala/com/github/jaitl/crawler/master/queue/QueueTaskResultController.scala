package com.github.jaitl.crawler.master.queue

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.pipe
import akka.cluster.sharding.ShardRegion
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.AddNewTasks
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsFailed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsProcessed
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.models.worker.CommonActions.ActionSuccess

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.Failure

class QueueTaskResultController(
  queueProvider: QueueTaskProvider,
  config: QueueTaskConfig
) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = {
    case MarkAsProcessed(requestId, taskType, ids, requester) =>
      val dropFuture = queueProvider.dropTasks(ids).map(_ => ActionSuccess(requestId, taskType))

      dropFuture pipeTo requester

      dropFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsProcessed, requestId: $requestId")
        case _ =>
      }

    case MarkAsFailed(requestId, taskType, ids, requester) =>
      val updateFuture = for {
        tasks <- queueProvider.getByIds(ids)
        failedTasks = tasks.filter(t => t.attempt + 1 >= config.maxAttemptsCount).map(_.id)
        recoveredTasks = tasks.filter(t => t.attempt + 1 < config.maxAttemptsCount).map(_.id)
        _ <- if (failedTasks.nonEmpty) {
          queueProvider.updateTasksStatusAndIncAttempt(failedTasks, TaskStatus.taskFailed)
        } else {
          Future.successful(Unit)
        }
        _ <- if (recoveredTasks.nonEmpty) {
          queueProvider.updateTasksStatusAndIncAttempt(recoveredTasks, TaskStatus.taskWait)
        } else {
          Future.successful(Unit)
        }
      } yield ActionSuccess(requestId, taskType)

      updateFuture pipeTo requester

      updateFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsFailed, requestId: $requestId")
        case _ =>
      }

    case AddNewTasks(requestId, taskType, tasksData, requester) =>
      val pushFuture = queueProvider.pushTasks(taskType, tasksData).map(_ => ActionSuccess(requestId, taskType))

      pushFuture pipeTo requester

      pushFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during AddNewTasks, requestId: $requestId")
        case _ =>
      }
  }
}

object QueueTaskResultController {
  case class MarkAsProcessed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class MarkAsFailed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class AddNewTasks(requestId: UUID, taskType: String, tasksData: Seq[String], requester: ActorRef)

  def props(queueProvider: QueueTaskProvider, config: QueueTaskConfig): Props =
    Props(new QueueTaskResultController(queueProvider, config))

  def name(): String = "queueTaskResultController"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ MarkAsProcessed(requestId, _, _, _) => (requestId.toString, msg)
    case msg @ MarkAsFailed(requestId, _, _, _) => (requestId.toString, msg)
    case msg @ AddNewTasks(requestId, _, _, _) => (requestId.toString, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case MarkAsProcessed(_, taskType, _, _) => taskType
    case MarkAsFailed(_, taskType, _, _) => taskType
    case AddNewTasks(_, taskType, _, _) => taskType
  }
}
