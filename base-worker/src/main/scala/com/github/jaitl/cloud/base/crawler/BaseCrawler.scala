package com.github.jaitl.cloud.base.crawler

import com.github.jaitl.cloud.base.http.HttpRequestExecutor

import scala.concurrent.Future

trait BaseCrawler {
  def crawl(task: CrawlTask, httpRequestExecutor: HttpRequestExecutor): Future[CrawlResult]
}

trait BaseCrawlerCreator {
  def create(): BaseCrawler
}
