package com.github.jaitl.cloud.simple.worker

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App  extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    logger.info("Start worker on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port"))


    val system = ActorSystem("cloudCrawlerSystem", config)
  }
}
