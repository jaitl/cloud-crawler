package com.github.jaitl.cloud.base.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import com.github.jaitl.cloud.base.crawler.CrawlResult
import com.github.jaitl.cloud.base.crawler.CrawlTask
import com.github.jaitl.cloud.base.executor.CrawlExecutor.Crawl
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlFailureResult
import com.github.jaitl.cloud.base.executor.CrawlExecutor.CrawlSuccessResult
import com.github.jaitl.cloud.base.http.HttpRequestExecutor
import com.github.jaitl.cloud.base.parser.ParseResult
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.task.Task

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
          val crawlResult = for {
            crawlResult <- Try(crawler.crawl(CrawlTask(task.taskData, task.taskType), requestExecutor)) match {
              case Success(res) => res
              case Failure(ex) => Future.failed(ex)
            }
            parseResult <- Future(pipeline.parser.map(parser => parser.parse(crawlResult)))
          } yield CrawlSuccessResult(requestId, task, requestExecutor, crawlResult, parseResult)

          val recoveredResult = crawlResult.recover {
            case t: Throwable => CrawlFailureResult(requestId, task, requestExecutor, t)
          }

          recoveredResult pipeTo sender()

        case Failure(ex) =>
          sender() ! CrawlFailureResult(requestId, task, requestExecutor, ex)
      }
  }
}

private[base] object CrawlExecutor {

  case class Crawl(requestId: UUID, task: Task, requestExecutor: HttpRequestExecutor, pipeline: Pipeline)

  case class CrawlSuccessResult(
    requestId: UUID,
    task: Task,
    requestExecutor: HttpRequestExecutor,
    crawlResult: CrawlResult,
    parseResult: Option[ParseResult]
  )

  case class CrawlFailureResult(
    requestId: UUID,
    task: Task,
    requestExecutor: HttpRequestExecutor,
    throwable: Throwable
  )

  def props(): Props = Props(new CrawlExecutor)

  def name(): String = "crawlExecutor"
}
