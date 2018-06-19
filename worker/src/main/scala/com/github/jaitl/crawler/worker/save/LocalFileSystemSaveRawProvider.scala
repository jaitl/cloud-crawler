package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = ???
}
