package com.github.jaitl.crawler.worker.executor

import java.util.UUID

import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import com.github.jaitl.crawler.master.client.task.Task
import com.github.jaitl.crawler.worker.ActorTestSuite
import com.github.jaitl.crawler.worker.client.QueueClient
import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.AddResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailedTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.FailureSaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessAddedResults
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessCrawledTask
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SuccessSavedResults
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.NewCrawlTasks
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.ResourceType
import com.github.jaitl.crawler.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.scheduler.Scheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SaveCrawlResultControllerTest extends ActorTestSuite {
  import scala.concurrent.duration._

  class OnlyRawResultSaverSuite {
    val tasksBatchSize = 10

    val baseCrawler = mock[BaseCrawler]
    val saveRawProvider = mock[SaveRawProvider]

    val pipeline = PipelineBuilder
      .noParserPipeline()
      .withTaskType("test")
      .withSaveRawProvider(saveRawProvider)
      .withCrawler(baseCrawler)
      .build()

    val queueTaskBalancer = TestProbe()
    val tasksBatchController = TestProbe()
    val queueClient = mock[QueueClient]

    val saveScheduler = mock[Scheduler]
    (saveScheduler.schedule _).expects(*, *, *)

    val config = SaveCrawlResultControllerConfig(1.minute)

    val saveCrawlResultController = TestActorRef[SaveCrawlResultController[TestDataRes]](
      SaveCrawlResultController.props(
        pipeline = pipeline,
        configPipeline = ConfigurablePipelineBuilder()
          .withProxy(mock[ResourceType])
          .withBatchSize(tasksBatchSize)
          .build(),
        queueClient = queueClient,
        tasksBatchController = tasksBatchController.ref,
        saveScheduler = saveScheduler,
        config = config
      ))
  }

  class OnlyParsedResultSaverSuite {
    val tasksBatchSize = 10

    val baseCrawler = mock[BaseCrawler]
    val saveParsedProvider = mock[SaveParsedProvider[TestDataRes]]
    val parser = mock[BaseParser[TestDataRes]]

    val pipeline = PipelineBuilder[TestDataRes]()
      .withTaskType("test")
      .withSaveResultProvider(saveParsedProvider)
      .withCrawler(baseCrawler)
      .withParser(parser)
      .build()

    val queueTaskBalancer = TestProbe()
    val tasksBatchController = TestProbe()
    val queueClient = mock[QueueClient]

    val saveScheduler = mock[Scheduler]
    (saveScheduler.schedule _).expects(*, *, *)

    val config = SaveCrawlResultControllerConfig(1.minute)

    val saveCrawlResultController = TestActorRef[SaveCrawlResultController[TestDataRes]](
      SaveCrawlResultController.props(
        pipeline = pipeline,
        configPipeline = ConfigurablePipelineBuilder()
          .withProxy(mock[ResourceType])
          .withBatchSize(tasksBatchSize)
          .build(),
        queueClient = queueClient,
        tasksBatchController = tasksBatchController.ref,
        saveScheduler = saveScheduler,
        config = config
      ))
  }

  class RawAndParsedResultSaverSuite {
    val tasksBatchSize = 10
    val baseCrawler = mock[BaseCrawler]
    val saveParsedProvider = mock[SaveParsedProvider[TestDataRes]]
    val parser = mock[BaseParser[TestDataRes]]
    val saveRawProvider = mock[SaveRawProvider]

    val pipeline = PipelineBuilder[TestDataRes]()
      .withTaskType("test")
      .withSaveResultProvider(saveParsedProvider)
      .withCrawler(baseCrawler)
      .withParser(parser)
      .withSaveRawProvider(saveRawProvider)
      .build()

    val queueTaskBalancer = TestProbe()
    val tasksBatchController = TestProbe()
    val queueClient = mock[QueueClient]

    val saveScheduler = mock[Scheduler]
    (saveScheduler.schedule _).expects(*, *, *)

    val config = SaveCrawlResultControllerConfig(1.minute)

    val saveCrawlResultController = TestActorRef[SaveCrawlResultController[TestDataRes]](
      SaveCrawlResultController.props(
        pipeline = pipeline,
        configPipeline = ConfigurablePipelineBuilder()
          .withProxy(mock[ResourceType])
          .withBatchSize(tasksBatchSize)
          .build(),
        queueClient = queueClient,
        tasksBatchController = tasksBatchController.ref,
        saveScheduler = saveScheduler,
        config = config
      ))
  }

  "AddResults" should {
    "SuccessCrawledTask" in new OnlyRawResultSaverSuite {
      val successCrawledTask = SuccessCrawledTask(
        Task("1", "1", "1"),
        CrawlResult("1"),
        Some(ParseResult[TestDataRes](TestDataRes("1")))
      )

      saveCrawlResultController ! AddResults(successCrawledTask)

      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.successTasks should have).size(1)
    }

    "FailedTask" in new OnlyRawResultSaverSuite {
      val failedTask = FailedTask(Task("1", "1", "1"), new Exception("error"))

      saveCrawlResultController ! AddResults(failedTask)

      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.failedTasks should have).size(1)
    }
  }

  "SaveResults" should {
    "save raw result" in new OnlyRawResultSaverSuite {
      (saveRawProvider.save _).expects(*).returning(futureSuccess)

      val successCrawledTask1 = SuccessCrawledTask(Task("1", "test", "1"), CrawlResult("1"), None)
      saveCrawlResultController ! AddResults(successCrawledTask1)
      expectMsg(SuccessAddedResults)

      val successCrawledTask2 = SuccessCrawledTask(Task("2", "test", "2"), CrawlResult("2"), None)
      saveCrawlResultController ! AddResults(successCrawledTask2)
      expectMsg(SuccessAddedResults)

      val successCrawledTask3 = SuccessCrawledTask(Task("3", "test", "3"), CrawlResult("3"), None)
      saveCrawlResultController ! AddResults(successCrawledTask3)
      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.successTasks should have).size(3)

      (queueClient.putProcessResult _)
        .expects(where {
          (
            requestId: UUID,
            successIds: Seq[String],
            failureIds: Seq[String],
            skippedIds: Seq[String],
            parsingFailedTaskIds: Seq[String],
            bannedIds: Seq[String],
            newTasks: Map[String, Seq[String]]) =>
            successIds.size == 3 && successIds == Seq("1", "2", "3") && failureIds.isEmpty
        })
        .returning(Future.successful(()))

      saveCrawlResultController ! SaveResults

      tasksBatchController.expectMsg(SuccessSavedResults)
    }

    "save parsed result" in new OnlyParsedResultSaverSuite {
      (saveParsedProvider.saveResults(_: Seq[TestDataRes])(_: ExecutionContext)).expects(*, *).returning(futureSuccess)

      val successCrawledTask1 = SuccessCrawledTask(
        Task("1", "test", "1"),
        CrawlResult("1"),
        Some(ParseResult(TestDataRes("1"), Seq(NewCrawlTasks("test1", Seq("1", "2", "3"))))))
      saveCrawlResultController ! AddResults(successCrawledTask1)
      expectMsg(SuccessAddedResults)

      val successCrawledTask2 = SuccessCrawledTask(
        Task("2", "test", "2"),
        CrawlResult("2"),
        Some(ParseResult(TestDataRes("2"), Seq(NewCrawlTasks("test2", Seq("21", "22", "23"))))))
      saveCrawlResultController ! AddResults(successCrawledTask2)
      expectMsg(SuccessAddedResults)

      val successCrawledTask3 =
        SuccessCrawledTask(Task("3", "test", "3"), CrawlResult("3"), Some(ParseResult(TestDataRes("3"))))
      saveCrawlResultController ! AddResults(successCrawledTask3)
      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.successTasks should have).size(3)

      (queueClient.putProcessResult _)
        .expects(where {
          (
            requestId: UUID,
            successIds: Seq[String],
            failureIds: Seq[String],
            skippedIds: Seq[String],
            parsingFailedTaskIds: Seq[String],
            bannedIds: Seq[String],
            newTasks: Map[String, Seq[String]]) =>
            successIds.size == 3 && successIds == Seq("1", "2", "3") && failureIds.isEmpty &&
            newTasks.keySet == Set("test1", "test2") && newTasks("test1") == Seq("1", "2", "3") &&
            newTasks("test2") == Seq("21", "22", "23")
        })
        .returning(Future.successful(()))

      saveCrawlResultController ! SaveResults

      tasksBatchController.expectMsg(SuccessSavedResults)
    }

    "save raw and parsed result" in new RawAndParsedResultSaverSuite {
      (saveParsedProvider.saveResults(_: Seq[TestDataRes])(_: ExecutionContext)).expects(*, *).returning(futureSuccess)
      (saveRawProvider.save _).expects(*).returning(futureSuccess)

      val successCrawledTask1 = SuccessCrawledTask(
        Task("1", "test", "1"),
        CrawlResult("1"),
        Some(ParseResult(TestDataRes("1"), Seq(NewCrawlTasks("test1", Seq("1", "2", "3"))))))
      saveCrawlResultController ! AddResults(successCrawledTask1)
      expectMsg(SuccessAddedResults)

      val successCrawledTask2 = SuccessCrawledTask(
        Task("2", "test", "2"),
        CrawlResult("2"),
        Some(ParseResult(TestDataRes("2"), Seq(NewCrawlTasks("test2", Seq("21", "22", "23"))))))
      saveCrawlResultController ! AddResults(successCrawledTask2)
      expectMsg(SuccessAddedResults)

      val successCrawledTask3 =
        SuccessCrawledTask(Task("3", "test", "3"), CrawlResult("3"), Some(ParseResult(TestDataRes("3"))))
      saveCrawlResultController ! AddResults(successCrawledTask3)
      expectMsg(SuccessAddedResults)

      saveCrawlResultController ! AddResults(FailedTask(Task("4", "test", "4"), new Exception))
      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.successTasks should have).size(3)
      (saveCrawlResultController.underlyingActor.failedTasks should have).size(1)

      (queueClient.putProcessResult _)
        .expects(where {
          (
            requestId: UUID,
            successIds: Seq[String],
            failureIds: Seq[String],
            skippedIds: Seq[String],
            parsingFailedTaskIds: Seq[String],
            bannedIds: Seq[String],
            newTasks: Map[String, Seq[String]]) =>
            successIds.size == 3 && successIds.containsSlice(Seq("1", "2", "3")) && failureIds == Seq("4") &&
            newTasks.keySet == Set("test1", "test2") && newTasks("test1") == Seq("1", "2", "3") &&
            newTasks("test2") == Seq("21", "22", "23")
        })
        .returning(Future.successful(()))

      saveCrawlResultController ! SaveResults

      tasksBatchController.expectMsg(SuccessSavedResults)
    }

    "failure during save" in new OnlyParsedResultSaverSuite {
      (saveParsedProvider
        .saveResults(_: Seq[TestDataRes])(_: ExecutionContext))
        .expects(*, *)
        .returning(Future.failed(new Exception("")))

      val successCrawledTask1 =
        SuccessCrawledTask(Task("1", "test", "1"), CrawlResult("1"), Some(ParseResult(TestDataRes("1"))))
      saveCrawlResultController ! AddResults(successCrawledTask1)
      expectMsg(SuccessAddedResults)

      (saveCrawlResultController.underlyingActor.successTasks should have).size(1)
      (saveCrawlResultController.underlyingActor.failedTasks should have).size(0)

      saveCrawlResultController ! SaveResults

      tasksBatchController.expectMsgType[FailureSaveResults]
    }

    "try to save empty results" in new OnlyParsedResultSaverSuite {
      (saveParsedProvider.saveResults(_: Seq[TestDataRes])(_: ExecutionContext)).expects(*, *).never()

      (saveCrawlResultController.underlyingActor.successTasks should have).size(0)
      (saveCrawlResultController.underlyingActor.failedTasks should have).size(0)

      saveCrawlResultController ! SaveResults
      tasksBatchController.expectMsg(SuccessSavedResults)
    }
  }

  case class TestDataRes(data: String)
}
