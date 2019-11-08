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
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsParsingFailed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsProcessed
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.MarkAsSkipped
import com.github.jaitl.crawler.master.queue.QueueTaskResultController.ReturnToQueue
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
) extends Actor
    with ActorLogging {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = markTasks.orElse(addTasks).orElse(returnTasks)

  private val markTasks: Receive = {
    case MarkAsProcessed(requestId, taskType, ids, requester) =>
      val dropFuture = queueProvider.dropTasks(ids).map(_ => ActionSuccess(requestId, taskType))

      dropFuture.pipeTo(requester)

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

      updateFuture.pipeTo(requester)

      updateFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsFailed, requestId: $requestId")
        case _ =>
      }

    case MarkAsSkipped(requestId, taskType, ids, requester) =>
      val updateFuture = for {
        tasks <- queueProvider.getByIds(ids)
        _ <- if (tasks.nonEmpty) {
          queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskSkipped)
        } else {
          Future.successful(Unit)
        }
      } yield ActionSuccess(requestId, taskType)

      updateFuture.pipeTo(requester)

      updateFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsSkipped, requestId: $requestId")
        case _ =>
      }
    case MarkAsParsingFailed(requestId, taskType, ids, requester) =>
      val updateFuture = for {
        tasks <- queueProvider.getByIds(ids)
        _ <- if (tasks.nonEmpty) {
          queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskParsingFailed)
        } else {
          Future.successful(Unit)
        }
      } yield ActionSuccess(requestId, taskType)

      updateFuture.pipeTo(requester)

      updateFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsSkipped, requestId: $requestId")
        case _ =>
      }
  }

  private val addTasks: Receive = {
    case AddNewTasks(requestId, taskType, tasksData, requester) =>
      val pushFuture = queueProvider.pushTasks(taskType, tasksData).map(_ => ActionSuccess(requestId, taskType))

      pushFuture.pipeTo(requester)

      pushFuture.onComplete {
        case Failure(ex) => log.error(ex, s"Error during AddNewTasks, requestId: $requestId")
        case _ =>
      }
  }

  private val returnTasks: Receive = {
    case ReturnToQueue(requestId, taskType, ids, requester) =>
      val returnFuture = queueProvider
        .updateTasksStatus(ids, TaskStatus.taskWait)
        .map(_ => ActionSuccess(requestId, taskType))

      returnFuture.pipeTo(requester)
  }
}

object QueueTaskResultController {
  case class MarkAsProcessed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class MarkAsParsingFailed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class MarkAsFailed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class MarkAsSkipped(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class AddNewTasks(requestId: UUID, taskType: String, tasksData: Seq[String], requester: ActorRef)

  case class ReturnToQueue(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  def props(queueProvider: QueueTaskProvider, config: QueueTaskConfig): Props =
    Props(new QueueTaskResultController(queueProvider, config))

  def name(): String = "queueTaskResultController"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ MarkAsProcessed(_, taskType, _, _) => (taskType, msg)
    case msg @ MarkAsFailed(_, taskType, _, _) => (taskType, msg)
    case msg @ MarkAsParsingFailed(_, taskType, _, _) => (taskType, msg)
    case msg @ MarkAsSkipped(_, taskType, _, _) => (taskType, msg)
    case msg @ AddNewTasks(_, taskType, _, _) => (taskType, msg)
    case msg @ ReturnToQueue(_, taskType, _, _) => (taskType, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case MarkAsProcessed(_, taskType, _, _) => taskType
    case MarkAsFailed(_, taskType, _, _) => taskType
    case MarkAsParsingFailed(_, taskType, _, _) => taskType
    case MarkAsSkipped(_, taskType, _, _) => taskType
    case AddNewTasks(_, taskType, _, _) => taskType
    case ReturnToQueue(_, taskType, _, _) => taskType
  }
}
