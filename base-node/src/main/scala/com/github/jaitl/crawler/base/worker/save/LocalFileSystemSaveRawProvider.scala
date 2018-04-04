package com.github.jaitl.crawler.base.worker.save

import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.crawler.CrawlTask

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(raw: Seq[(CrawlTask, CrawlResult)]): Future[Unit] = ???
}
