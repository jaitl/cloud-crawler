package com.github.jaitl.crawler.master.queue

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.cluster.sharding.ShardRegion
import akka.pattern.pipe
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController.QueueBatchFailure
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController.QueueBatchSuccess
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController.RequestTask
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksBatchRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

class QueueTaskRequestController(
  queueProvider: QueueTaskProvider
) extends Actor with ActorLogging with Stash {
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def receive: Receive = waitRequest

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

  private def processingRequest: Receive = {
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

    case req@RequestTask(_, _, _, _) =>
      log.debug(s"stash req: $req")
      stash()
  }
}

object QueueTaskRequestController {

  case class RequestTask(requestId: UUID, taskType: String, batchSize: Int,requester: ActorRef)

  case class QueueBatchSuccess(requestId: UUID, taskType: String, requester: ActorRef, tasks: Seq[Task])

  case class QueueBatchFailure(requestId: UUID, taskType: String, requester: ActorRef, throwable: Throwable)

  def props(queueProvider: QueueTaskProvider): Props = Props(new QueueTaskRequestController(queueProvider))

  def name(): String = "queueTaskRequestController"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@RequestTask(requestId, _, _, _) => (requestId.toString, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case RequestTask(_, taskType, _, _) => taskType
  }
}
