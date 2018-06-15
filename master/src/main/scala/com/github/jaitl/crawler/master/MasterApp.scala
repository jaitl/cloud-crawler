package com.github.jaitl.crawler.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.crawler.master.queue.QueueTaskBalancer
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController
import com.github.jaitl.crawler.master.queue.QueueTaskResultController
import com.github.jaitl.crawler.master.queue.provider.MongoQueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object MasterApp extends StrictLogging {
  def main(args: Array[String]): Unit = {
    // TODO move to config
    val taskProvider: QueueTaskProvider = new MongoQueueTaskProvider(
      "mongodb://localhost:27017", "cloud_master", "CrawlTasks"
    )

    val config = ConfigFactory.load("master.conf")

    logger.info(
      "Start master on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem("cloudCrawlerSystem", config)

    val queueTaskQueueReqCtrl: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskRequestController.name(),
      entityProps = QueueTaskRequestController.props(taskProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskRequestController.extractEntityId,
      extractShardId = QueueTaskRequestController.extractShardId
    )

    val queueTaskQueueResCtrl: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskResultController.name(),
      entityProps = QueueTaskResultController.props(taskProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskResultController.extractEntityId,
      extractShardId = QueueTaskResultController.extractShardId
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskBalancer.props(queueTaskQueueReqCtrl, queueTaskQueueResCtrl),
        terminationMessage = QueueTaskBalancer.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskBalancer.name()
    )
  }
}
