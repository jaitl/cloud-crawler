package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.cloud.base.crawler.BaseCrawler
import com.github.jaitl.cloud.base.crawler.BaseCrawlerCreator
import com.github.jaitl.cloud.base.crawler.CrawlResult
import com.github.jaitl.cloud.base.crawler.CrawlTask
import com.github.jaitl.cloud.base.http.HttpRequestExecutor

import scala.concurrent.Future

class HabrCrawler extends BaseCrawler {
  override def crawl(task: CrawlTask, httpRequestExecutor: HttpRequestExecutor): Future[CrawlResult] = ???
}

object HabrCrawlerCreator extends BaseCrawlerCreator {
  override def create(): BaseCrawler = new HabrCrawler
}
