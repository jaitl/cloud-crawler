package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.StackoverflowCrawler
import com.github.jaitl.cloud.simple.worker.parser.{StackoverflowElasticsearchConverter, StackoverflowParser}
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.ElasticSearchSaveParsedProvider
import com.github.jaitl.crawler.worker.save.S3SaveRawProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._

object AppSO extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val elasticSearchTypeConverter = new StackoverflowElasticsearchConverter()
    val batchSize = 10
    val torPort = 9050

    val habrPipeline = PipelineBuilder()
      .withTaskType("StackoverflowTasks")
      .withBatchSize(batchSize)
      .withCrawler(new StackoverflowCrawler)
      .withParser(new StackoverflowParser)
      .withSaveResultProvider(new ElasticSearchSaveParsedProvider("localhost", "so", 9200, "docker-cluster"))
      //.withSaveRawProvider(new LocalFileSystemSaveRawProvider("./"))
      .withSaveRawProvider(new S3SaveRawProvider(
      "AKIAJNM66J3T7O6FZDNA",
      "uk7BlybsPLDEWNUHHQhajoWRlM2fivaZYUvGp0fm",
      "avc-cloud-crawler"
    ))
      .withTor("127.0.0.1", torPort, 2, RandomTimeout(2.seconds, 1.seconds))
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addPipelines(pipelines)
      .parallelBatches(2)
      .run()
  }
}
