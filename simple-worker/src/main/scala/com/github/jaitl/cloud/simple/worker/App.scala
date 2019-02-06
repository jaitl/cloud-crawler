package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawler
import com.github.jaitl.cloud.simple.worker.parser.HabrDataMongoConverter
import com.github.jaitl.cloud.simple.worker.parser.HabrParser
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.MongoSaveParsedProvider
import com.github.jaitl.crawler.worker.save.S3SaveRawProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val habrConverter = new HabrDataMongoConverter()
    val batchSize = 10
    val torPort = 9050
    val torControlPort = 9051

    val habrPipeline = PipelineBuilder()
      .withTaskType("HabrTasks")
      .withBatchSize(batchSize)
      .withCrawler(new HabrCrawler)
      .withParser(new HabrParser)
      .withSaveResultProvider(
        new MongoSaveParsedProvider("mongodb://root:example@localhost:27017", "CrawlResults", "HabrData"))
      //.withSaveRawProvider(new LocalFileSystemSaveRawProvider("./"))
      .withSaveRawProvider(
        new S3SaveRawProvider(
          "",
          "",
          ""
        ))
      .withTor("127.0.0.1", torPort, 2, RandomTimeout(2.seconds, 1.seconds), torControlPort, "")
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(2)
      .run()
  }
}
