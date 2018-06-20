package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HabrCrawler extends BaseCrawler {
  val habrUrl = "https://habr.com/post"
  override def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult] =
    httpRequestExecutor.get(s"$habrUrl/${task.taskData}/")
      .filter(_.code == 200)
      .map(res => CrawlResult(res.body))
}
