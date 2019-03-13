package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.StackoverflowCrawler
import com.github.jaitl.cloud.simple.worker.parser.SODataMongoConverter
import com.github.jaitl.cloud.simple.worker.parser.StackoverflowParser
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.LocalFileSystemSaveRawProvider
import com.github.jaitl.crawler.worker.save.MongoSaveParsedProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object AppSOMongo extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val soMongoConverter =
      new SODataMongoConverter()
    val config = ConfigFactory.load("simple-worker.conf")

    val habrPipeline = PipelineBuilder()
      .withTaskType(config.getString("simple-worker.taskType"))
      .withBatchSize(config.getString("simple-worker.batchSize").toInt)
      .withCrawler(new StackoverflowCrawler(config.getString("simple-worker.baseUrl")))
      .withParser(new StackoverflowParser)
      .withSaveResultProvider(
        new MongoSaveParsedProvider(
          config.getString("simple-worker.mongo.url"),
          config.getString("simple-worker.mongo.db"),
          config.getString("simple-worker.mongo.collection")))
      .withSaveRawProvider(new LocalFileSystemSaveRawProvider(config.getString("simple-worker.filepath")))
      .withTor(
        config.getString("simple-worker.tor.host"),
        config.getString("simple-worker.tor.port").toInt,
        config.getString("simple-worker.tor.limit").toInt,
        RandomTimeout(
          Duration.fromNanos(config.getDuration("simple-worker.tor.timeout.up").toNanos),
          Duration.fromNanos(config.getDuration("simple-worker.tor.timeout.down").toNanos)
        )
      )
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(config.getString("simple-worker.parallel").toInt)
      .run()
  }
}
