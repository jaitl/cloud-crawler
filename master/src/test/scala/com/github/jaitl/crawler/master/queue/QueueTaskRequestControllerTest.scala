package com.github.jaitl.crawler.master.queue

import java.util.UUID

import com.github.jaitl.crawler.master.ActorTestSuite
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController.RequestTask
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.TaskStatus
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksBatchRequest

import scala.concurrent.Future
import scala.concurrent.Promise

class QueueTaskRequestControllerTest extends ActorTestSuite {

  "QueueTaskRequestController" should {
    "success request task" in {
      val requestId = UUID.randomUUID()
      val taskType = "type1"
      val batchSize = 10

      val tasks = Seq(
        Task("1", taskType, "tt1"),
        Task("2", taskType, "tt2")
      )
      val tasksBatch = TasksBatch(requestId, taskType, tasks)

      val queueProvider = mock[QueueTaskProvider]
      (queueProvider.pullBatch _).expects(taskType, batchSize).returning(Future.successful(tasks))
      (queueProvider.updateTasksStatus _).expects(tasks.map(_.id), TaskStatus.taskInProgress).returning(futureSuccess)

      val queueTaskController = system.actorOf(QueueTaskRequestController.props(queueProvider))

      queueTaskController ! RequestTask(requestId, taskType, batchSize, self)

      expectMsg(SuccessTasksBatchRequest(requestId, taskType, tasksBatch))
    }

    "no task" in {
      val requestId = UUID.randomUUID()
      val taskType = "type1"
      val batchSize = 10

      val tasks = Seq.empty

      val queueProvider = mock[QueueTaskProvider]
      (queueProvider.pullBatch _).expects(taskType, batchSize).returning(Future.successful(tasks))

      val queueTaskController = system.actorOf(QueueTaskRequestController.props(queueProvider))

      queueTaskController ! RequestTask(requestId, taskType, batchSize, self)

      expectMsg(NoTasks(requestId, taskType))
    }

    "failure request task" in {
      val requestId = UUID.randomUUID()
      val taskType = "type1"
      val batchSize = 10

      val t = new Exception()

      val queueProvider = mock[QueueTaskProvider]
      (queueProvider.pullBatch _).expects(taskType, batchSize).returning(Future.failed(t))

      val queueTaskController = system.actorOf(QueueTaskRequestController.props(queueProvider))

      queueTaskController ! RequestTask(requestId, taskType, batchSize, self)

      expectMsg(FailureTasksBatchRequest(requestId, taskType, t))
    }

    "two concurrent task requests" in {
      val requestId1 = UUID.randomUUID()
      val requestId2 = UUID.randomUUID()
      val taskType = "type1"
      val batchSize = 10

      val tasks = Seq.empty

      val pullPromise1 = Promise[Seq[Task]]()
      val pullPromise2 = Promise[Seq[Task]]()

      val queueProvider = mock[QueueTaskProvider]
      (queueProvider.pullBatch _).expects(taskType, batchSize).returning(pullPromise1.future)
      (queueProvider.pullBatch _).expects(taskType, batchSize).returning(pullPromise2.future)

      val queueTaskController = system.actorOf(QueueTaskRequestController.props(queueProvider))

      queueTaskController ! RequestTask(requestId1, taskType, batchSize, self)
      queueTaskController ! RequestTask(requestId2, taskType, batchSize, self)

      pullPromise1.success(tasks)

      expectMsg(NoTasks(requestId1, taskType))

      pullPromise2.success(tasks)

      expectMsg(NoTasks(requestId2, taskType))
    }
  }
}
