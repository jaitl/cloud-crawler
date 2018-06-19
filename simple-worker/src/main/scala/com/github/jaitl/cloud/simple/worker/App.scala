package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawler
import com.github.jaitl.cloud.simple.worker.parser.HabrParser
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.LocalFileSystemSaveRawProvider
import com.github.jaitl.crawler.worker.save.MongoSaveParsedProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val batchSize = 100
    val proxyLimit = 10

    val habrPipeline = PipelineBuilder()
      .withTaskType("HabrTasks")
      .withBatchSize(batchSize)
      .withCrawler(new HabrCrawler)
      .withParser(new HabrParser)
      .withSaveResultProvider(new MongoSaveParsedProvider("HabrResult"))
      .withSaveRawProvider(new LocalFileSystemSaveRawProvider("/data/crawler/habr"))
      .withProxy(proxyLimit, RandomTimeout(3.seconds, 1.second))
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(2)
      .run()
  }
}
