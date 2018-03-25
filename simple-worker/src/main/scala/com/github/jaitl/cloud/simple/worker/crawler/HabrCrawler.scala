package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.crawler.base.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.base.worker.crawler.BaseCrawlerCreator
import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.crawler.CrawlTask
import com.github.jaitl.crawler.base.worker.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HabrCrawler extends BaseCrawler {
  override def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult] = ???
}

object HabrCrawlerCreator extends BaseCrawlerCreator {
  override def create(): BaseCrawler = new HabrCrawler
}
