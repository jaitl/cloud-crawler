package com.github.jaitl.crawler.base.master.queue

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.cluster.sharding.ShardRegion
import akka.pattern.pipe
import com.github.jaitl.crawler.base.models.task.Task
import com.github.jaitl.crawler.base.models.task.TasksBatch
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.AddNewTasks
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.MarkAsFailed
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.MarkAsProcessed
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.QueueBatchFailure
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.QueueBatchSuccess
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.QueueTaskControllerConfig
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.RequestTask
import com.github.jaitl.crawler.base.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.base.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.base.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.base.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.base.worker.WorkerManager.SuccessTasksBatchRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

class QueueTaskController(
  queueProvider: QueueTaskProvider,
  config: QueueTaskControllerConfig
) extends Actor with ActorLogging with Stash {
  implicit val ec = ExecutionContext.global

  override def receive: Receive = waitRequest orElse processResult

  private def waitRequest: Receive = {
    case RequestTask(requestId, taskType, batchSize, requester) =>
      log.debug(s"RequestTask: $requestId, $taskType")

      context.become(processingRequest)

      val batchResult = for {
        tasks <- queueProvider.pullBatch(taskType, batchSize)
        _ <- if (tasks.nonEmpty) {
          queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskInProgress)
        } else {
          Future.successful(Unit)
        }
      } yield QueueBatchSuccess(requestId, taskType, requester, tasks)

      batchResult
        .recover { case t: Throwable => QueueBatchFailure(requestId, taskType, requester, t) }
        .pipeTo(self)
  }

  private def processResult: Receive = {
    case MarkAsProcessed(requestId, taskType, ids, requester) =>
      queueProvider.dropTasks(ids).onComplete {
        case Failure(ex) => log.error(ex, s"Error during MarkAsProcessed, requestId: $requestId")
        case _ =>
      }

    case MarkAsFailed(requestId, taskType, ids, requester) =>
    // TODO drop from cache

    case AddNewTasks(requestId, taskType, tasksData, requester) =>
      queueProvider.pushTasks(taskType, tasksData).onComplete {
        case Failure(ex) => log.error(ex, s"Error during AddNewTasks, requestId: $requestId")
        case _ =>
      }
  }

  private def processingRequest: Receive = processResult orElse {
    case QueueBatchSuccess(requestId, taskType, requester, tasks) =>

      if (tasks.nonEmpty) {
        val tasksBatch = TasksBatch(requestId, taskType, tasks)
        requester ! SuccessTasksBatchRequest(requestId, taskType, tasksBatch)
      } else {
        requester ! NoTasks(requestId, taskType)
      }

      context.unbecome()
      unstashAll()

    case QueueBatchFailure(requestId, taskType, requester, throwable) =>
      requester ! FailureTasksBatchRequest(requestId, taskType, throwable)

      context.unbecome()
      unstashAll()

    case RequestTask(_, _, _, _) => stash()
  }
}

object QueueTaskController {

  case class RequestTask(requestId: UUID, taskType: String, batchSize: Int,requester: ActorRef)

  case class QueueBatchSuccess(requestId: UUID, taskType: String, requester: ActorRef, tasks: Seq[Task])

  case class QueueBatchFailure(requestId: UUID, taskType: String, requester: ActorRef, throwable: Throwable)

  case class MarkAsProcessed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class MarkAsFailed(requestId: UUID, taskType: String, ids: Seq[String], requester: ActorRef)

  case class AddNewTasks(requestId: UUID, taskType: String, tasksData: Seq[String], requester: ActorRef)

  def props(queueProvider: QueueTaskProvider, config: QueueTaskControllerConfig): Props =
    Props(new QueueTaskController(queueProvider, config))

  def name(): String = "queueTaskController"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@RequestTask(requestId, _, _, _) => (requestId.toString, msg)
    case msg@MarkAsProcessed(requestId, _, _, _) => (requestId.toString, msg)
    case msg@MarkAsFailed(requestId, _, _, _) => (requestId.toString, msg)
    case msg@AddNewTasks(requestId, _, _, _) => (requestId.toString, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case RequestTask(_, taskType, _, _) => taskType
    case MarkAsProcessed(_, taskType, _, _) => taskType
    case MarkAsFailed(_, taskType, _, _) => taskType
    case AddNewTasks(_, taskType, _, _) => taskType
  }

  case class QueueTaskControllerConfig()

}
