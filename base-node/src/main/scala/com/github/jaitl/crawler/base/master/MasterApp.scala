package com.github.jaitl.crawler.base.master

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer
import com.github.jaitl.crawler.base.master.queue.QueueTaskRequestController
import com.github.jaitl.crawler.base.master.queue.QueueTaskResultController
import com.github.jaitl.crawler.base.master.queue.provider.QueueTaskProvider
import com.typesafe.scalalogging.StrictLogging

class MasterApp(system: ActorSystem, taskProvider: QueueTaskProvider) extends StrictLogging {
  def run(): Unit = {
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
