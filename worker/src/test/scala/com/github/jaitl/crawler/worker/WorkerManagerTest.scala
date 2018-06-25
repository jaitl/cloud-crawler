package com.github.jaitl.crawler.worker

import java.util.UUID

import akka.actor.Terminated
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.TaskTypeWithBatchSize
import com.github.jaitl.crawler.worker.WorkerManager.CheckTimeout
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.scheduler.Scheduler
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

class WorkerManagerTest extends ActorTestSuite {
  import scala.concurrent.duration._

  class WorkerTestSuite {
    val pipeline = PipelineBuilder
      .noParserPipeline()
      .withTaskType("test")
      .withBatchSize(10)
      .withSaveRawProvider(mock[SaveRawProvider])
      .withCrawler(mock[BaseCrawler])
      .withTor("0", 0, 1, RandomTimeout(1.millis, 1.millis))
      .build()

    val queueTaskBalancer = TestProbe()
    val pipelines = Map("test" -> pipeline)
    val config = WorkerConfig(1, 5.seconds, 1.minute, 1.millis)
    val tasksBatchController = TestProbe()
    val tasksBatchControllerCreator = mock[TwoArgumentActorCreator[TasksBatch, Pipeline[_]]]
    val batchRequestScheduler = mock[Scheduler]
    val batchExecutionTimeoutScheduler = mock[Scheduler]

    (batchRequestScheduler.schedule _).expects(*, *, *).returning(Unit)
    (batchExecutionTimeoutScheduler.schedule _).expects(*, *, *).returning(Unit)
    (tasksBatchControllerCreator.create _).expects(*, *, *).returning(tasksBatchController.ref)

    val workerManager = TestActorRef[WorkerManager](WorkerManager.props(
      queueTaskBalancer.ref, pipelines, config, tasksBatchControllerCreator, batchRequestScheduler,
      batchExecutionTimeoutScheduler
    ))
  }

  "WorkerManager" should {
    "RequestBatch" in new WorkerTestSuite {
      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      res.taskTypes should contain only TaskTypeWithBatchSize("test", 10)

      queueTaskBalancer.reply(SuccessTasksBatchRequest(
        res.requestId, "test", TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
      ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      workerManager.underlyingActor.batchControllers should have size 1
    }

    "Terminated" in new WorkerTestSuite {
      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      res.taskTypes should contain only TaskTypeWithBatchSize("test", 10)

      queueTaskBalancer.reply(SuccessTasksBatchRequest(
        res.requestId, "test", TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
      ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      workerManager.underlyingActor.batchControllers should have size 1

      system.stop(tasksBatchController.ref)

      Thread.sleep(200)

      workerManager.underlyingActor.batchControllers should have size 0
    }

    "CheckTimeout" in new WorkerTestSuite {
      val watcher = TestProbe()
      watcher.watch(tasksBatchController.ref)

      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      res.taskTypes should contain only TaskTypeWithBatchSize("test", 10)

      queueTaskBalancer.reply(SuccessTasksBatchRequest(
        res.requestId, "test", TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
      ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      workerManager.underlyingActor.batchControllers should have size 1

      Thread.sleep(200)

      workerManager ! CheckTimeout

      watcher.expectTerminated(tasksBatchController.ref)

      workerManager.underlyingActor.batchControllers should have size 0
    }
  }
}
