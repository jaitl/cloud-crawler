package com.github.jaitl.crawler.worker.executor

import java.io.IOException
import java.util.UUID

import akka.actor.ActorRef
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.worker.ActorTestSuite
import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.creator.ActorCreator
import com.github.jaitl.crawler.worker.creator.OneArgumentActorCreator
import com.github.jaitl.crawler.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.Crawl
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.worker.executor.TasksBatchController.ExecuteTask
import com.github.jaitl.crawler.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.NoResourcesAvailable
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.RequestResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnFailedResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.ReturnSuccessResource
import com.github.jaitl.crawler.worker.executor.resource.ResourceController.SuccessRequestResource
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.ResourceType
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.scheduler.Scheduler
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

import scala.concurrent.duration._

class TasksBatchControllerTest extends ActorTestSuite {
  class TestSuite(taskCount: Int) {
    val tasks = (1 to taskCount).map(i => Task(i.toString, "test", i.toString))
    val batch = TasksBatch(
      id = UUID.randomUUID(),
      taskType = "test",
      tasks = tasks
    )

    val pipeline = PipelineBuilder[TestDataRes]()
      .withTaskType("test")
      .withBatchSize(10)
      .withSaveRawProvider(mock[SaveRawProvider])
      .withCrawler(mock[BaseCrawler])
      .withTor("0", 0, 1, RandomTimeout(1.millis, 1.millis))
      .build()

    val resourceController = TestProbe()
    val resourceControllerCreator = mock[OneArgumentActorCreator[ResourceType]]
    val crawlExecutor = TestProbe()
    val crawlExecutorCreator = mock[ActorCreator]
    val saveCrawlResultController = TestProbe()
    val saveCrawlResultCreator = mock[TwoArgumentActorCreator[Pipeline[_], ActorRef]]
    val executeScheduler = mock[Scheduler]
    val config = TasksBatchControllerConfig(maxAttempts = 2, 1.minute)

    (resourceControllerCreator.create _).expects(*, *).returning(resourceController.ref)
    (crawlExecutorCreator.create _).expects(*).returning(crawlExecutor.ref)
    (saveCrawlResultCreator.create _).expects(*, *, *).returning(saveCrawlResultController.ref)
    (executeScheduler.schedule _).expects(*, *, *).returning(Unit)

    val tasksBatchController = TestActorRef[TasksBatchController](
      TasksBatchController.props(batch, pipeline, resourceControllerCreator, crawlExecutorCreator,
        saveCrawlResultCreator, executeScheduler, config)
    )

    val requestExecutor = mock[HttpRequestExecutor]
  }

  "TasksBatchController" should {
    "success batch crawl" in new TestSuite(1) {
      val watcher = TestProbe()
      watcher.watch(tasksBatchController)

      tasksBatchController.underlyingActor.taskQueue should have size 1
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController ! ExecuteTask

      var resRequest = resourceController.expectMsgType[RequestResource]
      resourceController.reply(SuccessRequestResource(resRequest.requestId, requestExecutor))

      var crawlRequest = crawlExecutor.expectMsgType[Crawl]

      tasksBatchController.underlyingActor.taskQueue should have size 0
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 1

      crawlExecutor.reply(CrawlSuccessResult(
        crawlRequest.requestId,
        crawlRequest.task,
        crawlRequest.requestExecutor,
        CrawlResult("1"),
        Some(ParseResult(TestDataRes("1")))
      ))

      resourceController.expectMsg(ReturnSuccessResource(resRequest.requestId, requestExecutor))
      saveCrawlResultController.expectMsg(AddResults(SuccessCrawledTask(
        crawlRequest.task.task, CrawlResult("1"), Some(ParseResult(TestDataRes("1")))
      )))

      saveCrawlResultController.reply(SuccessAddedResults)

      tasksBatchController.underlyingActor.taskQueue should have size 0
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController ! ExecuteTask

      saveCrawlResultController.expectMsg(SaveResults)
      saveCrawlResultController.reply(SuccessSavedResults)

      watcher.expectTerminated(tasksBatchController)
    }

    "forcedStop" in new TestSuite(2) {
      val watcher = TestProbe()
      watcher.watch(tasksBatchController)

      tasksBatchController.underlyingActor.forcedStop shouldBe false
      tasksBatchController.underlyingActor.taskQueue should have size 2
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController ! ExecuteTask

      var resRequest = resourceController.expectMsgType[RequestResource]
      resourceController.reply(NoResourcesAvailable(resRequest.requestId))

      saveCrawlResultController.expectMsg(SaveResults)

      tasksBatchController.underlyingActor.forcedStop shouldBe true
      tasksBatchController.underlyingActor.taskQueue should have size 2
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      saveCrawlResultController.reply(SuccessSavedResults)

      watcher.expectTerminated(tasksBatchController)
    }

    "resource fail during crawl" in new TestSuite(2) {
      tasksBatchController.underlyingActor.taskQueue should have size 2
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController ! ExecuteTask

      var resRequest = resourceController.expectMsgType[RequestResource]
      resourceController.reply(SuccessRequestResource(resRequest.requestId, requestExecutor))

      var crawlRequest = crawlExecutor.expectMsgType[Crawl]

      tasksBatchController.underlyingActor.taskQueue should have size 1
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 1

      crawlExecutor.reply(CrawlFailureResult(
        crawlRequest.requestId, crawlRequest.task, crawlRequest.requestExecutor, new IOException("")
      ))

      resourceController.expectMsgType[ReturnFailedResource]

      tasksBatchController.underlyingActor.taskQueue should have size 2
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0
    }

    "exception during crawl" in new TestSuite(1) {
      tasksBatchController.underlyingActor.taskQueue should have size 1
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController ! ExecuteTask

      var resRequest = resourceController.expectMsgType[RequestResource]
      resourceController.reply(SuccessRequestResource(resRequest.requestId, requestExecutor))

      var crawlRequest = crawlExecutor.expectMsgType[Crawl]

      tasksBatchController.underlyingActor.taskQueue should have size 0
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 1

      var ex = new Exception("")
      crawlExecutor.reply(CrawlFailureResult(
        crawlRequest.requestId, crawlRequest.task, crawlRequest.requestExecutor, ex
      ))

      resourceController.expectMsgType[ReturnSuccessResource]

      tasksBatchController.underlyingActor.taskQueue should have size 1
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 0

      tasksBatchController.underlyingActor.taskQueue.head.attempt shouldBe 1
      tasksBatchController.underlyingActor.taskQueue.head.t should contain only (ex)

      tasksBatchController ! ExecuteTask

      resRequest = resourceController.expectMsgType[RequestResource]
      resourceController.reply(SuccessRequestResource(resRequest.requestId, requestExecutor))

      crawlRequest = crawlExecutor.expectMsgType[Crawl]

      tasksBatchController.underlyingActor.taskQueue should have size 0
      tasksBatchController.underlyingActor.currentActiveCrawlTask shouldBe 1

      ex = new Exception("")

      crawlExecutor.reply(CrawlFailureResult(
        crawlRequest.requestId, crawlRequest.task, crawlRequest.requestExecutor, ex
      ))

      resourceController.expectMsgType[ReturnSuccessResource]

      saveCrawlResultController.expectMsg(AddResults(FailedTask(crawlRequest.task.task, ex)))
    }
  }

  case class TestDataRes(data: String)
}
