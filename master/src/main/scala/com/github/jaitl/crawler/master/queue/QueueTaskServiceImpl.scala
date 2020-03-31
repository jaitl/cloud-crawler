package com.github.jaitl.crawler.master.queue

import com.github.jaitl.crawler.master.client.queue.ProcessResultReply
import com.github.jaitl.crawler.master.client.queue.ProcessResultRequest
import com.github.jaitl.crawler.master.client.queue.ReturnReply
import com.github.jaitl.crawler.master.client.queue.ReturnRequest
import com.github.jaitl.crawler.master.client.queue.TaskQueueServiceGrpc
import com.github.jaitl.crawler.master.client.queue.TaskReply
import com.github.jaitl.crawler.master.client.queue.TaskRequest
import com.github.jaitl.crawler.master.client.task.Task
import com.github.jaitl.crawler.master.client.task.TaskTypeWithBatchSize
import com.github.jaitl.crawler.master.client.task.TasksBatch
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.typesafe.scalalogging.StrictLogging
import io.grpc.BindableService
import io.grpc.ServerServiceDefinition

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.util.Try

class QueueTaskServiceImpl(
  queueProvider: QueueTaskProvider,
  config: QueueTaskConfig
)(implicit executionContext: ExecutionContext)
    extends TaskQueueServiceGrpc.TaskQueueService
    with StrictLogging
    with BindableService {
  override def getTasks(request: TaskRequest): Future[TaskReply] = Future.successful {
    if (request.taskTypes.nonEmpty) {
      val batch = if (request.taskTypes.lengthCompare(0) == 1) {
        request.taskTypes.head
      } else {
        Random.shuffle(request.taskTypes.toIndexedSeq).head
      }
      requestTasks(batch) match {
        case Success(tasks) if tasks.nonEmpty =>
          logger.info(s"found tasks, count: ${tasks.length}, taskType: ${batch.taskType}")
          val tasksBatch = TasksBatch(request.requestId, batch.taskType, tasks)
          TaskReply(status = TaskReply.Status.OK, tasksBatch = Some(tasksBatch))
        case Success(_) =>
          logger.info(s"no tasks, taskType: ${batch.taskType}")
          TaskReply(status = TaskReply.Status.NO_TASKS)
        case Failure(ex) =>
          logger.error(s"Error during db request, requestId: ${request.requestId}", ex)
          TaskReply(status = TaskReply.Status.FAILED)
      }
    } else {
      logger.error(s"attempt to request tasks with empty tasks list, requestId: ${request.requestId}")
      TaskReply(status = TaskReply.Status.FAILED_EMPTY_TASKS_LIST)
    }
  }

  private def requestTasks(batch: TaskTypeWithBatchSize): Try[Seq[Task]] = Try {
    this.synchronized {
      val tasks = Await.result(queueProvider.pullBatch(batch.taskType, batch.batchSize), config.dbRequestTimeout)
      if (tasks.nonEmpty) {
        val updateFuture = queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskInProgress)
        Await.result(updateFuture, config.dbRequestTimeout)
      }
      tasks
    }
  }

  override def returnToQueue(request: ReturnRequest): Future[ReturnReply] =
    queueProvider
      .updateTasksStatus(request.ids, TaskStatus.taskWait)
      .map(_ => ReturnReply(ReturnReply.Status.OK))
      .recover {
        case ex: Throwable =>
          logger.error(s"Failed return to queue, requestId: ${request.requestId}", ex)
          ReturnReply(ReturnReply.Status.FAILED)
      }

  override def putProcessResult(request: ProcessResultRequest): Future[ProcessResultReply] =
    for {
      _ <- markAsProcessed(request.requestId, request.successIds ++ request.bannedIds)
      _ <- markAsFailed(request.requestId, request.failureIds)
      _ <- markAsSkipped(request.requestId, request.skippedIds)
      _ <- markAsParsingFailed(request.requestId, request.failureIds)
      _ <- addNewTasks(request.requestId, request.newTasks.map { case (t, s) => t -> s.tasks })
    } yield ProcessResultReply(ProcessResultReply.Status.OK)

  private def markAsProcessed(requestId: String, ids: Seq[String]): Future[Unit] =
    queueProvider.dropTasks(ids).recover {
      case ex: Throwable =>
        logger.error(s"Fail to mark as processed, ids: [${ids.mkString(",")}], requestId: $requestId", ex)
    }

  private def markAsFailed(requestId: String, ids: Seq[String]): Future[Unit] = {
    val updateFuture = for {
      tasks <- queueProvider.getByIds(ids)
      failedTasks = tasks.filter(t => t.attempt + 1 >= config.maxAttemptsCount).map(_.id)
      recoveredTasks = tasks.filter(t => t.attempt + 1 < config.maxAttemptsCount).map(_.id)
      _ <- if (failedTasks.nonEmpty) {
        queueProvider.updateTasksStatusAndIncAttempt(failedTasks, TaskStatus.taskFailed)
      } else {
        Future.successful(())
      }
      _ <- if (recoveredTasks.nonEmpty) {
        queueProvider.updateTasksStatusAndIncAttempt(recoveredTasks, TaskStatus.taskWait)
      } else {
        Future.successful(())
      }
    } yield ()

    updateFuture.recover {
      case ex: Throwable =>
        logger.error(s"Fail to mark as failed, ids: [${ids.mkString(",")}], requestId: $requestId", ex)
    }
  }

  private def markAsSkipped(requestId: String, ids: Seq[String]): Future[Unit] = {
    val updateFuture = for {
      tasks <- queueProvider.getByIds(ids)
      _ <- if (tasks.nonEmpty) {
        queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskSkipped)
      } else {
        Future.successful(())
      }
    } yield ()

    updateFuture.recover {
      case ex: Throwable =>
        logger.error(s"Fail to mark as skipped, ids: [${ids.mkString(",")}], requestId: $requestId", ex)
    }
  }
  private def markAsParsingFailed(requestId: String, ids: Seq[String]): Future[Unit] = {
    val updateFuture = for {
      tasks <- queueProvider.getByIds(ids)
      _ <- if (tasks.nonEmpty) {
        queueProvider.updateTasksStatus(tasks.map(_.id), TaskStatus.taskParsingFailed)
      } else {
        Future.successful(())
      }
    } yield ()

    updateFuture.recover {
      case ex: Throwable =>
        logger.error(s"Fail to mark as parsingFailed, ids: [${ids.mkString(",")}], requestId: $requestId", ex)
    }
  }
  private def addNewTasks(requestId: String, taskData: Map[String, Seq[String]]): Future[Unit] =
    queueProvider.pushTasks(taskData).recover {
      case ex: Throwable =>
        logger.error(s"Fail to add new tasks, taskData: $taskData], requestId: $requestId", ex)
    }

  override def bindService(): ServerServiceDefinition =
    TaskQueueServiceGrpc.bindService(new QueueTaskServiceImpl(queueProvider, config), executionContext)
}
