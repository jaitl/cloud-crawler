package com.github.jaitl.crawler.worker.client

import java.util.UUID

import com.github.jaitl.crawler.master.client.queue.ProcessResultReply
import com.github.jaitl.crawler.master.client.queue.ProcessResultRequest
import com.github.jaitl.crawler.master.client.queue.ReturnReply
import com.github.jaitl.crawler.master.client.queue.ReturnRequest
import com.github.jaitl.crawler.master.client.queue.TaskQueueServiceGrpc
import com.github.jaitl.crawler.master.client.queue.TaskReply
import com.github.jaitl.crawler.master.client.queue.TaskRequest
import com.github.jaitl.crawler.master.client.task.NewTasks
import com.github.jaitl.crawler.master.client.task.TaskTypeWithBatchSize
import com.github.jaitl.crawler.master.client.task.TasksBatch

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class QueueClientImpl(
  taskQueueClient: TaskQueueServiceGrpc.TaskQueueServiceStub
)(implicit executionContext: ExecutionContext)
    extends QueueClient {
  override def getTasks(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize]): Future[Option[TasksBatch]] =
    taskQueueClient
      .getTasks(TaskRequest(requestId.toString, taskTypes))
      .map { reply =>
        reply.status match {
          case TaskReply.Status.OK => reply.tasksBatch
          case TaskReply.Status.NO_TASKS => None
          case TaskReply.Status.FAILED_EMPTY_TASKS_LIST => throw new Exception(s"Empty taskTypesList, id: $requestId")
          case TaskReply.Status.FAILED => throw new Exception(s"Master failure, examine master logs, id: $requestId")
          case status: TaskReply.Status => throw new Exception(s"Unknown status: $status, id: $requestId")
        }
      }

  override def returnTasks(requestId: UUID, ids: Seq[String]): Future[Unit] =
    taskQueueClient
      .returnToQueue(ReturnRequest(requestId.toString, ids))
      .map { reply =>
        reply.status match {
          case ReturnReply.Status.OK => Unit
          case ReturnReply.Status.FAILED => throw new Exception(s"Master failure, examine master logs, id: $requestId")
          case status: ReturnReply.Status => throw new Exception(s"Unknown status: $status, id: $requestId")
        }
      }

  override def putProcessResult(
    requestId: UUID,
    successIds: Seq[String],
    failureIds: Seq[String],
    skippedIds: Seq[String],
    parsingFailedTaskIds: Seq[String],
    bannedIds: Seq[String],
    newTasks: Map[String, Seq[String]]): Future[Unit] =
    taskQueueClient
      .putProcessResult(
        ProcessResultRequest(
          requestId = requestId.toString,
          successIds = successIds,
          failureIds = failureIds,
          skippedIds = skippedIds,
          parsingFailedTaskIds = parsingFailedTaskIds,
          bannedIds = bannedIds,
          newTasks = newTasks.map { case (t, s) => t -> NewTasks(s) }
        ))
      .map { reply =>
        reply.status match {
          case ProcessResultReply.Status.OK => Unit
          case ProcessResultReply.Status.FAILED =>
            throw new Exception(s"Master failure, examine master logs, id: $requestId")
          case status: ProcessResultReply.Status =>
            throw new Exception(s"Unknown status: $status, id: $requestId")
        }
      }
}
