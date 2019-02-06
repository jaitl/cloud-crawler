package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.crawler.worker.crawler.{BaseCrawler, CrawlResult, CrawlTask}
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor

import scala.concurrent.{ExecutionContext, Future}

class StackoverflowCrawler extends BaseCrawler {
  val soUrl = "https://stackoverflow.com/questions/"
  override def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult] =
    httpRequestExecutor.get(s"$soUrl/${task.taskData}/")
      .map { r =>
        if (r.code != 200) {
          throw new Exception(s"wrong http code: ${r.code}, body: ${r.body}")
        }
        r
      }
      .map(res => CrawlResult(res.body))
}
