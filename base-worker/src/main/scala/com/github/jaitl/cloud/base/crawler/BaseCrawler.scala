package com.github.jaitl.cloud.base.crawler

import scala.concurrent.Future

trait BaseCrawler {
  def crawl(task: CrawlTask): Future[CrawlResult]
}

trait BaseCrawlerCreator {
  def create(): BaseCrawler
}
