package com.github.jaitl.crawler.base.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer
import com.github.jaitl.crawler.base.master.queue.QueueTaskController
import com.github.jaitl.crawler.base.master.queue.QueueTaskController.QueueTaskControllerConfig
import com.github.jaitl.crawler.base.master.queue.provider.QueueTaskProvider
import com.typesafe.scalalogging.StrictLogging

class MasterApp(system: ActorSystem, taskProvider: QueueTaskProvider) extends StrictLogging {
  def run(): Unit = {
    // TODO read from application.conf
    val batchSize = QueueTaskControllerConfig() // scalastyle:ignore

    val typedQueueManager: ActorRef = ClusterSharding(system).start(
      typeName = QueueTaskController.name(),
      entityProps = QueueTaskController.props(taskProvider, batchSize),
      settings = ClusterShardingSettings(system).withRole("master"),
      extractEntityId = QueueTaskController.extractEntityId,
      extractShardId = QueueTaskController.extractShardId
    )

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = QueueTaskBalancer.props(typedQueueManager),
        terminationMessage = QueueTaskBalancer.Stop,
        settings = ClusterSingletonManagerSettings(system).withRole("master")
      ),
      name = QueueTaskBalancer.name()
    )
  }
}
