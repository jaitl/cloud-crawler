package com.github.jaitl.crawler.worker.executor

import java.util.UUID

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.ActorTestSuite
import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.Crawl
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.crawler.worker.executor.TasksBatchController.QueuedTask
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CrawlExecutorTest extends ActorTestSuite {
  import scala.concurrent.duration._

  class ExecutorTestSuite {
    val baseCrawler = mock[BaseCrawler]
    val parser = mock[BaseParser[TestDataRes]]
    val saveParsedProvider = mock[SaveParsedProvider[TestDataRes]]
    val requestExecutor = mock[HttpRequestExecutor]

    val pipeline = PipelineBuilder[TestDataRes]()
      .withTaskType("test")
      .withBatchSize(10)
      .withCrawler(baseCrawler)
      .withParser(parser)
      .withSaveResultProvider(saveParsedProvider)
      .withTor("0", 0, 1, RandomTimeout(1.millis, 1.millis), 0, "")
      .build()
  }

  "CrawlExecutor" should {
    "success crawl" in new ExecutorTestSuite {
      (baseCrawler
        .crawl(_: CrawlTask, _: HttpRequestExecutor)(_: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(CrawlResult("1")))

      (parser.parse _).expects(*, *).returning(ParseResult(TestDataRes("1")))

      val crawlExecutor = system.actorOf(CrawlExecutor.props())

      crawlExecutor ! Crawl(UUID.randomUUID(), QueuedTask(Task("1", "test", "1"), 0), requestExecutor, pipeline)

      val res = expectMsgType[CrawlSuccessResult]

      res.crawlResult shouldBe CrawlResult("1")
      res.parseResult shouldBe Some(ParseResult(TestDataRes("1")))
    }

    "crawl throw exception" in new ExecutorTestSuite {
      (baseCrawler
        .crawl(_: CrawlTask, _: HttpRequestExecutor)(_: ExecutionContext))
        .expects(*, *, *)
        .throwing(new Exception(""))

      (parser.parse _).expects(*, *).never()

      val crawlExecutor = system.actorOf(CrawlExecutor.props())

      crawlExecutor ! Crawl(UUID.randomUUID(), QueuedTask(Task("1", "test", "1"), 0), requestExecutor, pipeline)

      expectMsgType[CrawlFailureResult]
    }

    "crawl failed" in new ExecutorTestSuite {
      (baseCrawler
        .crawl(_: CrawlTask, _: HttpRequestExecutor)(_: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(new Exception("")))

      (parser.parse _).expects(*, *).never()

      val crawlExecutor = system.actorOf(CrawlExecutor.props())

      crawlExecutor ! Crawl(UUID.randomUUID(), QueuedTask(Task("1", "test", "1"), 0), requestExecutor, pipeline)

      expectMsgType[CrawlFailureResult]
    }

    "parser failed" in new ExecutorTestSuite {
      (baseCrawler
        .crawl(_: CrawlTask, _: HttpRequestExecutor)(_: ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(CrawlResult("1")))

      (parser.parse _).expects(*, *).throwing(new Exception(""))

      val crawlExecutor = system.actorOf(CrawlExecutor.props())

      crawlExecutor ! Crawl(UUID.randomUUID(), QueuedTask(Task("1", "test", "1"), 0), requestExecutor, pipeline)

      expectMsgType[CrawlFailureResult]
    }
  }

  case class TestDataRes(data: String)
}
