package com.github.jaitl.crawler.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.crawler.master.queue.QueueTaskBalancer
import com.github.jaitl.crawler.master.queue.QueueTaskConfig
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController
import com.github.jaitl.crawler.master.queue.QueueTaskResultController
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProviderFactory
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object MasterApp extends StrictLogging {
  import net.ceedubs.ficus.Ficus._ // scalastyle:ignore
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._ // scalastyle:ignore

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("master.conf")

    val taskProvider: QueueTaskProvider = QueueTaskProviderFactory
      .getProvider(config.getConfig("master.task-provider"))

    logger.info(
      "Start master on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem("cloudCrawlerSystem", config)

    val queueTaskConfig = config.as[QueueTaskConfig]("master.queue-task")

    val queueTaskQueueReqCtrl: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskRequestController.name(),
      entityProps = QueueTaskRequestController.props(taskProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskRequestController.extractEntityId,
      extractShardId = QueueTaskRequestController.extractShardId
    )

    val queueTaskQueueResCtrl: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskResultController.name(),
      entityProps = QueueTaskResultController.props(taskProvider, queueTaskConfig),
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
