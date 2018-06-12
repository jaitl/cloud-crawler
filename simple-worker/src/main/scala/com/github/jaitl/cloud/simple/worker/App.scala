package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawlerCreator
import com.github.jaitl.cloud.simple.worker.parser.HabrParser
import com.github.jaitl.crawler.base.NodeApp
import com.github.jaitl.crawler.base.master.queue.provider.MongoQueueTaskProvider
import com.github.jaitl.crawler.base.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.base.worker.save.LocalFileSystemSaveRawProvider
import com.github.jaitl.crawler.base.worker.save.MongoSaveParsedProvider
import com.github.jaitl.crawler.base.worker.timeout.RandomTimeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val batchSize = 100
    val proxyLimit = 10

    val habrPipeline = PipelineBuilder()
      .withTaskType("HabrTasks")
      .withBatchSize(batchSize)
      .withCrawlerCreator(HabrCrawlerCreator)
      .withParser(new HabrParser)
      .withSaveResultProvider(new MongoSaveParsedProvider("HabrResult"))
      .withSaveRawProvider(new LocalFileSystemSaveRawProvider("/data/crawler/habr"))
      .withProxy(proxyLimit, RandomTimeout(3.seconds, 1.second))
      .build()

    val pipelines = habrPipeline :: Nil

    NodeApp
      .addTaskProvider(new MongoQueueTaskProvider("mongodb://localhost:27017", "cloud_master", "CrawlTasks"))
      .addPipelines(pipelines)
      .addParallelBatches(2)
      .run()
  }
}
