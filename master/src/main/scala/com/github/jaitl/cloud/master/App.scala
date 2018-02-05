package com.github.jaitl.cloud.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.cloud.base.QueueTaskBalancerSingletonMessages
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    logger.info(
      "Start master on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem("cloudCrawlerSystem", config)

    val typedQueueManager: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskTypedManager.name(),
      entityProps = QueueTaskTypedManager.props(),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskTypedManager.extractEntityId,
      extractShardId = QueueTaskTypedManager.extractShardId
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskBalancerSingleton.props(typedQueueManager),
        terminationMessage = QueueTaskBalancerSingletonMessages.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskBalancerSingleton.name()
    )

    system.actorOf(Props(new ClusterDomainEventListener), "cluster-listener")
  }
}
