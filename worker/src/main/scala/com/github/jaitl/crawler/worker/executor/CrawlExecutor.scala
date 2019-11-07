package com.github.jaitl.crawler.worker.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.Crawl
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.crawler.worker.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.crawler.worker.executor.TasksBatchController.QueuedTask
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor
import com.github.jaitl.crawler.worker.parser.ParseResult
import com.github.jaitl.crawler.worker.pipeline.Pipeline

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

private class CrawlExecutor extends Actor {
  implicit private val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case Crawl(requestId, task, requestExecutor, pipeline) =>
      val crawlTask = CrawlTask(task.task.taskData, task.task.taskType)
      val crawlResult = for {
        crawlResult <- Try(pipeline.crawler.crawl(crawlTask, requestExecutor)) match {
          case Success(res) => res
          case Failure(ex) => Future.failed(ex)
        }
        parseResult <- Future(pipeline.parser.map(parser => parser.parse(crawlTask, crawlResult)))
      } yield CrawlSuccessResult(requestId, task, requestExecutor, crawlResult, parseResult)

      val recoveredResult = crawlResult.recover {
        case t: Throwable => CrawlFailureResult(requestId, task, requestExecutor, t)
      }

      recoveredResult.pipeTo(sender())
  }
}

private[worker] object CrawlExecutor {

  case class Crawl(
    requestId: UUID,
    task: QueuedTask,
    requestExecutor: HttpRequestExecutor,
    pipeline: Pipeline[_]
  )

  case class CrawlSuccessResult(
    requestId: UUID,
    task: QueuedTask,
    requestExecutor: HttpRequestExecutor,
    crawlResult: CrawlResult,
    parseResult: Option[ParseResult[_]]
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
