package com.github.jaitl.crawler.worker.save

import java.io.File
import java.io.RandomAccessFile

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String)
    extends SaveRawProvider
    with StrictLogging {
  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = Future {
    raw.toList.par.foreach(r => {
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
