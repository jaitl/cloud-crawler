package com.github.jaitl.cloud.simple.worker

import java.util.UUID

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.cloud.base.QueueTaskBalancerSingletonMessages
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App  extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    logger.info("Start worker on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port"))


    val system = ActorSystem("cloudCrawlerSystem", config)

    val queueTaskBalancer: ActorRef = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/queueTaskBalancer",
        settings = ClusterSingletonProxySettings(system).withRole("master")),
      name = "queueTaskBalancerProxy")

    queueTaskBalancer ! QueueTaskBalancerSingletonMessages.GetWork(UUID.randomUUID(), Seq("type1", "type2"))
    queueTaskBalancer ! QueueTaskBalancerSingletonMessages.GetWork(UUID.randomUUID(), Seq("type1", "type2"))

    // system.actorOf(PiplineWorker.props, PiplineWorker.name(UUID.randomUUID()))
  }
}
