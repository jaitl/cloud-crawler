package com.github.jaitl.crawler.worker

import java.util.UUID

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
import com.github.jaitl.crawler.worker.creator.ThreeArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipeline
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.ResourceType
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.scheduler.Scheduler
import com.github.jaitl.crawler.worker.validators.DummyTasksValidator
import org.scalatest.concurrent.Eventually

class WorkerManagerTest extends ActorTestSuite with Eventually {
  import scala.concurrent.duration._

  class WorkerTestSuite {
    val tasksBatchSize = 10

    val pipeline = PipelineBuilder
      .noParserPipeline()
      .withTaskType("test")
      .withSaveRawProvider(mock[SaveRawProvider])
      .withCrawler(mock[BaseCrawler])
      .build()

    val queueTaskBalancer = TestProbe()
    val pipelines = Map("test" -> pipeline)
    val config = WorkerConfig(1, 5.seconds, 1.minute, 1.millis)
    val tasksBatchController = TestProbe()
    val tasksBatchControllerCreator = mock[ThreeArgumentActorCreator[TasksBatch, Pipeline[_], ConfigurablePipeline]]
    val batchRequestScheduler = mock[Scheduler]
    val batchExecutionTimeoutScheduler = mock[Scheduler]
    val batchTasksValidator = new DummyTasksValidator

    (batchRequestScheduler.schedule _).expects(*, *, *).returning(Unit)
    (batchExecutionTimeoutScheduler.schedule _).expects(*, *, *).returning(Unit)
    (tasksBatchControllerCreator.create _).expects(*, *, *, *).returning(tasksBatchController.ref)

    val workerManager = TestActorRef[WorkerManager](
      WorkerManager.props(
        queueTaskBalancer.ref,
        pipelines,
        ConfigurablePipelineBuilder()
          .withProxy(mock[ResourceType])
          .withBatchSize(tasksBatchSize)
          .build(),
        config,
        tasksBatchControllerCreator,
        batchRequestScheduler,
        batchExecutionTimeoutScheduler,
        batchTasksValidator
      ))
  }

  "WorkerManager" should {
    "RequestBatch" in new WorkerTestSuite {
      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      (res.taskTypes should contain).only(TaskTypeWithBatchSize("test", 10))

      queueTaskBalancer.reply(
        SuccessTasksBatchRequest(
          res.requestId,
          "test",
          TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
        ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      (workerManager.underlyingActor.batchControllers should have).size(1)
    }

    "Terminated" in new WorkerTestSuite {
      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      (res.taskTypes should contain).only(TaskTypeWithBatchSize("test", 10))

      queueTaskBalancer.reply(
        SuccessTasksBatchRequest(
          res.requestId,
          "test",
          TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
        ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      (workerManager.underlyingActor.batchControllers should have).size(1)

      system.stop(tasksBatchController.ref)

      eventually {
        (workerManager.underlyingActor.batchControllers should have).size(0)
      }
    }

    "CheckTimeout" in new WorkerTestSuite {
      val watcher = TestProbe()
      watcher.watch(tasksBatchController.ref)

      workerManager ! RequestBatch

      val res = queueTaskBalancer.expectMsgType[RequestTasksBatch]
      (res.taskTypes should contain).only(TaskTypeWithBatchSize("test", 10))

      queueTaskBalancer.reply(
        SuccessTasksBatchRequest(
          res.requestId,
          "test",
          TasksBatch(UUID.randomUUID(), "test", Seq(Task("1", "test", "1")))
        ))

      tasksBatchController.expectMsg(TasksBatchController.ExecuteTask)

      (workerManager.underlyingActor.batchControllers should have).size(1)

      Thread.sleep(200)

      workerManager ! CheckTimeout

      watcher.expectTerminated(tasksBatchController.ref)

      (workerManager.underlyingActor.batchControllers should have).size(0)
    }
  }
}
