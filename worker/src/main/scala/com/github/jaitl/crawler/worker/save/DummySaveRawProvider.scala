package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.master.client.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DummySaveRawProvider(val path: String) extends SaveRawProvider with StrictLogging {
  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = Future {
    raw.toList.foreach(r => {})
  }
}
