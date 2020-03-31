package com.github.jaitl.crawler.worker.save

import java.io.File
import java.io.RandomAccessFile

import com.github.jaitl.crawler.master.client.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LocalFileSystemSaveRawProvider() extends SaveRawProvider with StrictLogging {

  private var path: String = ""
  def withPath(path: String): this.type = {
    this.path = path
    this
  }

  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = Future {
    raw.toList.foreach(r => {
      val folder = path
        .concat("/")
        .concat(r._1.id)
        .concat("/")
        .concat(r._1.taskType)
        .concat("/")
      new File(folder).mkdirs()
      val fileName = folder
        .concat(r._1.taskData)
      val ra = new RandomAccessFile(fileName, "rw")
      ra.write(r._2.data.getBytes)
      ra.close()
      logger.debug(s"Saving crawler result to: $fileName")
    })
  }
}
