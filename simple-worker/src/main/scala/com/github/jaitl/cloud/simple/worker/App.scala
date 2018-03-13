package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.base.WorkerApp
import com.github.jaitl.cloud.base.pipeline.PipelineBuilder
import com.github.jaitl.cloud.base.save.LocalFileSystemSaveRawProvider
import com.github.jaitl.cloud.base.save.MongoSaveParsedProvider
import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawlerCreator
import com.github.jaitl.cloud.simple.worker.parser.HabrParser
import com.typesafe.scalalogging.StrictLogging

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
      .withProxy(proxyLimit)
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(2)
      .run()
  }
}
