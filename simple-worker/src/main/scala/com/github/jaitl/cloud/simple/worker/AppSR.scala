package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.StackoverflowCrawler
import com.github.jaitl.cloud.simple.worker.parser.StackoverflowElasticsearchConverter
import com.github.jaitl.cloud.simple.worker.parser.StackoverflowParser
import com.github.jaitl.cloud.simple.worker.parser.StackoverrunParser
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.ElasticSearchSaveParsedProvider
import com.github.jaitl.crawler.worker.save.LocalFileSystemSaveRawProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object AppSR extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val elasticSearchTypeConverter =
      new StackoverflowElasticsearchConverter()
    val config = ConfigFactory.load("simple-worker.conf")

    val habrPipeline = PipelineBuilder()
      .withTaskType(config.getString("simple-worker.taskType"))
      .withBatchSize(config.getString("simple-worker.batchSize").toInt)
      .withCrawler(
        new StackoverflowCrawler(config.getString("simple-worker.baseUrl")))
      .withParser(new StackoverrunParser)
      .withSaveResultProvider(new ElasticSearchSaveParsedProvider(
        config.getString("simple-worker.es.hostname"),
        config.getString("simple-worker.es.index"),
        config.getString("simple-worker.es.port").toInt,
        config.getString("simple-worker.es.clustername")
      ))
      //.withSaveRawProvider(new LocalFileSystemSaveRawProvider("./"))
      .withSaveRawProvider(new LocalFileSystemSaveRawProvider(
        config.getString("simple-worker.filepath")))
      .withProxy(
        config.getString("simple-worker.proxy.host"),
        config.getString("simple-worker.proxy.port").toInt,
        config.getString("simple-worker.proxy.limit").toInt,
        RandomTimeout(
          Duration.fromNanos(
            config.getDuration("simple-worker.proxy.timeout.up").toNanos),
          Duration.fromNanos(
            config.getDuration("simple-worker.proxy.timeout.down").toNanos)
        ),
        config.getString("simple-worker.proxy.login"),
        config.getString("simple-worker.proxy.password")
      )
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(config.getString("simple-worker.parallel").toInt)
      .run()
  }
}
