package com.github.jaitl.crawler.base.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.crawler.CrawlTask
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.Crawl
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController.QueuedTask
import com.github.jaitl.crawler.base.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.base.worker.parser.ParseResult
import com.github.jaitl.crawler.base.worker.parser.ParsedData
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

private class CrawlExecutor extends Actor {
  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case Crawl(requestId, task, requestExecutor, pipeline) =>
      val tryCrawler = Try(pipeline.crawlerCreator.create())

      tryCrawler match {
        case Success(crawler) =>
          val crawlTask = CrawlTask(task.task.taskData, task.task.taskType)
          val crawlResult = for {
            crawlResult <- Try(crawler.crawl(crawlTask, requestExecutor)) match {
              case Success(res) => res
              case Failure(ex) => Future.failed(ex)
            }
            parseResult <- Future(pipeline.parser.map(parser => parser.parse(crawlTask, crawlResult)))
          } yield CrawlSuccessResult(requestId, task, requestExecutor, crawlResult, parseResult)

          val recoveredResult = crawlResult.recover {
            case t: Throwable => CrawlFailureResult(requestId, task, requestExecutor, t)
          }

          recoveredResult pipeTo sender()

        // TODO kill recoveredResult by timeout

        case Failure(ex) =>
          sender() ! CrawlFailureResult(requestId, task, requestExecutor, ex)
      }
  }
}

private[base] object CrawlExecutor {

  case class Crawl(
    requestId: UUID,
    task: QueuedTask,
    requestExecutor: HttpRequestExecutor,
    pipeline: Pipeline[ParsedData]
  )

  case class CrawlSuccessResult[T <: ParsedData](
    requestId: UUID,
    task: QueuedTask,
    requestExecutor: HttpRequestExecutor,
    crawlResult: CrawlResult,
    parseResult: Option[ParseResult[T]]
  )

  case class CrawlFailureResult(
    requestId: UUID,
    task: QueuedTask,
    requestExecutor: HttpRequestExecutor,
    throwable: Throwable
  )

  def props(): Props = Props(new CrawlExecutor)

  def name(): String = "crawlExecutor"
}
