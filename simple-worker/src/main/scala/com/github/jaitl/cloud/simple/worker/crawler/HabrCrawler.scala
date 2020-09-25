package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HabrCrawler extends BaseCrawler {
  override def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult] =
    httpRequestExecutor.get(task.taskData)
      .map { r =>
        if (r.code != 200) {
          throw new Exception(s"wrong http code: ${r.code}, body: ${r.body}")
        }
        r
      }
      .map(res => CrawlResult(task.taskId, res.body))
}
