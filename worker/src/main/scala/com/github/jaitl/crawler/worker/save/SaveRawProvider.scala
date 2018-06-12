package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask

import scala.concurrent.Future

trait SaveRawProvider {
  def save(raw: Seq[(CrawlTask, CrawlResult)]): Future[Unit]
}
