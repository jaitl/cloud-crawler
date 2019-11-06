package com.github.jaitl.crawler.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.crawler.master.config.ConfigurationBalancer
import com.github.jaitl.crawler.master.config.ConfigurationRequestController
import com.github.jaitl.crawler.master.config.ProxyRequestController
import com.github.jaitl.crawler.master.config.ResourceBalancer
import com.github.jaitl.crawler.master.config.TorRequestController
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProviderFactory
import com.github.jaitl.crawler.master.queue.QueueTaskBalancer
import com.github.jaitl.crawler.master.queue.QueueTaskConfig
import com.github.jaitl.crawler.master.queue.QueueTaskRecover
import com.github.jaitl.crawler.master.queue.QueueTaskRecover.RecoveryConfig
import com.github.jaitl.crawler.master.queue.QueueTaskRequestController
import com.github.jaitl.crawler.master.queue.QueueTaskResultController
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProvider
import com.github.jaitl.crawler.master.queue.provider.QueueTaskProviderFactory
import com.github.jaitl.crawler.master.scheduler.AkkaScheduler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object MasterApp extends StrictLogging {
  import net.ceedubs.ficus.Ficus._ // scalastyle:ignore
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._ // scalastyle:ignore

  // scalastyle:off method.length
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("master.conf")

    val taskProvider: QueueTaskProvider = QueueTaskProviderFactory
      .getProvider(config.getConfig("master.task-provider"))

    val configProvider: CrawlerConfigurationProvider = CrawlerConfigurationProviderFactory
      .getProvider(config.getConfig("master.config-provider"))

    logger.info(
      "Start master on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem(config.getConfig("clustering.cluster").getString("name"), config)

    val queueTaskConfig = config.as[QueueTaskConfig]("master.queue-task")
    val recoveryConfig = config.as[RecoveryConfig]("master.queue-task-recovery")

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

    val configurationRequestController: ActorRef = ClusterSharding(system).start(
      typeName = ConfigurationRequestController.name(),
      entityProps = ConfigurationRequestController.props(configProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = ConfigurationRequestController.extractEntityId,
      extractShardId = ConfigurationRequestController.extractShardId
    )

    val proxyRequestController: ActorRef = ClusterSharding(system).start(
      typeName = ProxyRequestController.name(),
      entityProps = ProxyRequestController.props(configProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = ProxyRequestController.extractEntityId,
      extractShardId = ProxyRequestController.extractShardId
    )

    val torRequestController: ActorRef = ClusterSharding(system).start(
      typeName = TorRequestController.name(),
      entityProps = TorRequestController.props(configProvider),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = TorRequestController.extractEntityId,
      extractShardId = TorRequestController.extractShardId
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = ResourceBalancer.props(proxyRequestController, torRequestController),
        terminationMessage = QueueTaskBalancer.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = ResourceBalancer.name()
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskBalancer.props(queueTaskQueueReqCtrl, queueTaskQueueResCtrl),
        terminationMessage = QueueTaskBalancer.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskBalancer.name()
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskRecover.props(taskProvider, new AkkaScheduler(system), recoveryConfig),
        terminationMessage = QueueTaskRecover.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskRecover.name()
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = ConfigurationBalancer.props(configurationRequestController),
        terminationMessage = ConfigurationBalancer.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = ConfigurationBalancer.name()
    )
  }
}
