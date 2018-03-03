package com.github.jaitl.cloud.master

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.common.models.QueueTaskBalancerSingletonMessages.GetWork

import scala.util.Random

class QueueTaskBalancerSingleton(queueTaskTypedManager: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case GetWork(requestId, taskTypes) if taskTypes.nonEmpty =>
      log.info(s"GetWork, requestId: $requestId, types: $taskTypes")
      val taskType = Random.shuffle(taskTypes.toList).head
      queueTaskTypedManager ! QueueTaskTypedManager.RequestTask(requestId, taskType)
  }
}

object QueueTaskBalancerSingleton {

  def props(queueTaskTypedManager: ActorRef): Props = Props(new QueueTaskBalancerSingleton(queueTaskTypedManager))
  def name(): String = "queueTaskBalancer"
}
