package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(raw: Seq[(CrawlTask, CrawlResult)]): Future[Unit] = ???
}
