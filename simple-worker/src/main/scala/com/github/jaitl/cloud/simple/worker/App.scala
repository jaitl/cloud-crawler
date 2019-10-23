package com.github.jaitl.cloud.simple.worker

import com.github.jaitl.cloud.simple.worker.parser.HabrDataMongoConverter
import com.github.jaitl.crawler.worker.WorkerApp
import com.github.jaitl.crawler.worker.pipeline.WarmUpPipelineBuilder
import com.typesafe.scalalogging.StrictLogging

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val habrConverter = new HabrDataMongoConverter()

    val habrPipeline = WarmUpPipelineBuilder()
      .withTaskType("HabrTasks")
      .build()

    val pipelines = habrPipeline :: Nil

    WorkerApp
      .addWarmUpPipeline(pipelines)
      .run()
  }
}
