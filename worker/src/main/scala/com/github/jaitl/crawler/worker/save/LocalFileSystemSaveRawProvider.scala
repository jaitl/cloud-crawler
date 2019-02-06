package com.github.jaitl.crawler.worker.save

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult

import scala.concurrent.Future

class LocalFileSystemSaveRawProvider(val path: String) extends SaveRawProvider {
  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = {
    raw.foreach(r => {
      val fileName = path.concat(r._1.id).concat(r._1.taskType).concat(r._1.taskData)
      val file = new File(fileName)
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(r._2.data)
      bw.close()
      logger.debug(s"Saving crawler result to: $fileName")
    })
    Future.successful(())
  }
}
