package com.github.jaitl.cloud.master

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    logger.info("Start master on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port"))


    val system = ActorSystem("cloudCrawlerSystem", config)

    system.actorOf(Props(new ClusterDomainEventListener), "cluster-listener")
  }
}