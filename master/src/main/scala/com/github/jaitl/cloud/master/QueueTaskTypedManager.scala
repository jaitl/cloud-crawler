package com.github.jaitl.cloud.master

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.cluster.sharding.ShardRegion
import com.github.jaitl.cloud.master.QueueTaskTypedManager.RequestTask

class QueueTaskTypedManager extends Actor with ActorLogging {
  override def receive: Receive = {
    case RequestTask(requestId, taskType) =>
      log.info(s"RequestTask: $requestId, $taskType")
  }
}

object QueueTaskTypedManager {
  case class RequestTask(requestId: UUID, taskType: String)

  def props(): Props = Props(new QueueTaskTypedManager)
  def name(): String = "queueTaskTypedManager"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@RequestTask(requestId, _) => (requestId.toString, msg)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case RequestTask(_, taskType) => taskType
  }
}
