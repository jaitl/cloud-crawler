package com.github.jaitl.cloud.simple.worker.crawler

import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.exception.PageNotFoundException
import com.github.jaitl.crawler.worker.http.HttpRequestExecutor

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class StackoverflowCrawler(
  val soUrl: String
) extends BaseCrawler {
  override def crawl(
    task: CrawlTask,
    httpRequestExecutor: HttpRequestExecutor
  )(implicit executionContext: ExecutionContext): Future[CrawlResult] =
    httpRequestExecutor
      .get(s"$soUrl/${task.taskData}")
      .map { r =>
        if (r.code != 200) {
          if (r.code == 404) {
            throw new PageNotFoundException(
              s"wrong http code: ${r.code} for page: ${task.taskData}")
          }
          throw new Exception(s"wrong http code: ${r.code}, body: ${r.body}")
        }
        r
      }
      .map(res => CrawlResult(res.body))
}
