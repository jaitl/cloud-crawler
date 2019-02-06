package com.github.jaitl.crawler.worker.save

import java.io.File

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
                         val path: String,
                         val endpoint: String = "https://s3-us-west-1.amazonaws.com"
                       ) extends SaveRawProvider with StrictLogging {
  val credentials = new BasicAWSCredentials(accessKey, secretKey)
  val client = new AmazonS3Client(credentials)

  override def save(raw: Seq[(Task, CrawlResult)]): Future[Unit] = Future {
    raw.toList.par.foreach(r => {
      val fileName = path.concat(r._1.id).concat(r._1.taskType).concat(r._1.taskData)
      client.putObject(bucketName, r._1.taskType.concat("/").concat(r._1.id), new File(fileName))
      logger.debug(s"Saving crawler result to: $fileName")
    })
  }
}
