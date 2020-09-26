package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.crawler.HabrCrawler
import com.github.jaitl.cloud.simple.worker.parser.{HabrDataMongoConverter, HabrDataSQLConverter, HabrParsedData, HabrParser}
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.save.{DummySaveRawProvider, MongoSaveParsedProvider, SqlSaveParsedProvider}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val habrConverter = new HabrDataSQLConverter()
    val config = ConfigFactory.load("simple-worker.conf")

    val habrPipeline = PipelineBuilder()
      .withTaskType("HabrTasks")
      .withCrawler(new HabrCrawler())
      .withParser(new HabrParser())
      .withSaveResultProvider(
        new SqlSaveParsedProvider[HabrParsedData](
          config.getString("simple-worker.sql.connectionUrl"),
          config.getString("simple-worker.sql.driverName"),
          config.getString("simple-worker.sql.user"),
          config.getString("simple-worker.sql.password")))
      .withSaveRawProvider(new DummySaveRawProvider(""))
      .build()

    WorkerApp
      .addWarmUpPipeline(habrPipeline)
      .run()
  }
}
