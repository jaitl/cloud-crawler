package com.github.jaitl.crawler.base.worker.crawler

import com.github.jaitl.crawler.base.worker.http.HttpRequestExecutor

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
