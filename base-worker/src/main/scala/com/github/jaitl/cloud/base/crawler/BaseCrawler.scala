package com.github.jaitl.cloud.base.crawler

import com.github.jaitl.cloud.base.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait BaseCrawler {
  def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult]
}

trait BaseCrawlerCreator {
  def create(): BaseCrawler
}
