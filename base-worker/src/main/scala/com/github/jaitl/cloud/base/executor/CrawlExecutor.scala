package com.github.jaitl.cloud.base.executor

import java.util.UUID

import akka.actor.Actor
import akka.actor.Props
import com.github.jaitl.cloud.base.crawler.CrawlResult
import com.github.jaitl.cloud.base.executor.CrawlExecutor.Crawl
import com.github.jaitl.cloud.base.parser.ParseResult
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.resource.Resource
import com.github.jaitl.cloud.common.models.task.Task

private class CrawlExecutor extends Actor {
  override def receive: Receive = {
    case Crawl(requestId, task, resource, pipeline) =>

  }
}

private[base] object CrawlExecutor {

  case class Crawl(requestId: UUID, task: Task, resource: Resource, pipeline: Pipeline)

  case class CrawlSuccessResult(
    requestId: UUID,
    task: Task,
    resource: Resource,
    crawlResult: CrawlResult,
    parseResult: ParseResult
  )

  case class CrawlFailureResult(requestId: UUID, task: Task, resource: Resource, throwable: Throwable)

  def props(): Props = Props(new CrawlExecutor)

}
