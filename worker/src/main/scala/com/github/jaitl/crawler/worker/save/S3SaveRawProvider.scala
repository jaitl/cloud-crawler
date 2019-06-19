package com.github.jaitl.crawler.worker.save

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import com.github.jaitl.crawler.models.task.Task
import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class S3SaveRawProvider(
  val accessKey: String,
  val secretKey: String,
  val bucketName: String,
  val path: String = "",
  val endpoint: String = "https://s3-us-west-1.amazonaws.com"
) extends SaveRawProvider
    with StrictLogging {
  val credentials = new BasicAWSCredentials(accessKey, secretKey)
  val client = new AmazonS3Client(credentials)

  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = Future {
    raw.toList.par.foreach(r => {
      val fileName = path.concat(r._1.id).concat(r._1.taskType).concat(r._1.taskData)
      val file = File.createTempFile(fileName, "")
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(r._2.data)
      bw.close()
      client.putObject(bucketName, r._1.taskType.concat("/").concat(r._1.id).concat("/").concat(r._1.taskData), file)
      logger.debug(s"Saving crawler result to: $fileName")
    })
  }
}
