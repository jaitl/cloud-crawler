package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult

import scala.concurrent.Future

trait SaveRawProvider {
  def save(raw: Seq[(Task, CrawlResult)]): Future[Unit]
}
