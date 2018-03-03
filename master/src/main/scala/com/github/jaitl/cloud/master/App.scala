package com.github.jaitl.cloud.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.cloud.master.queue.QueueTaskBalancerSingleton
import com.github.jaitl.cloud.master.queue.QueueTaskController
import com.github.jaitl.cloud.master.queue.QueueTaskController.QueueTaskControllerConfig
import com.github.jaitl.cloud.master.queue.provider.MongoQueueTaskProvider
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

    val taskProvider = new MongoQueueTaskProvider("mongodb://localhost:27017", "cloud_master", "CrawlTasks")
    val batchSize = QueueTaskControllerConfig(2) // scalastyle:ignore

    val system = ActorSystem("cloudCrawlerSystem", config)

    val typedQueueManager: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskController.name(),
      entityProps = QueueTaskController.props(taskProvider, batchSize),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskController.extractEntityId,
      extractShardId = QueueTaskController.extractShardId
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskBalancerSingleton.props(typedQueueManager),
        terminationMessage = QueueTaskBalancerSingleton.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskBalancerSingleton.name()
    )

    system.actorOf(Props(new ClusterDomainEventListener), "cluster-listener")
  }
}
