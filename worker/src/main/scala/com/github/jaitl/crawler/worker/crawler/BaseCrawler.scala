package com.github.jaitl.crawler.worker.crawler

import com.github.jaitl.crawler.worker.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait BaseCrawler {
  def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult]
}
