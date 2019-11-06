package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawler
import com.github.jaitl.cloud.simple.worker.parser.HabrDataMongoConverter
import com.github.jaitl.cloud.simple.worker.parser.HabrParser
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.DummySaveRawProvider
import com.github.jaitl.crawler.worker.save.MongoSaveParsedProvider
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val habrConverter = new HabrDataMongoConverter()
    val config = ConfigFactory.load("simple-worker.conf")

    val habrPipeline = PipelineBuilder()
      .withTaskType("HabrTasks")
      .withCrawler(new HabrCrawler())
      .withParser(new HabrParser())
      .withSaveResultProvider(
        new MongoSaveParsedProvider(
          config.getString("simple-worker.mongo.url"),
          config.getString("simple-worker.mongo.db"),
          config.getString("simple-worker.mongo.collection")))
      .withSaveRawProvider(new DummySaveRawProvider(""))
      .build()

    WorkerApp
      .addWarmUpPipeline(habrPipeline)
      .run()
  }
}
