package com.github.jaitl.cloud.master.queue

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.common.models.request.EmptyTaskTypeList
import com.github.jaitl.cloud.common.models.request.RequestTasksBatch
import com.github.jaitl.cloud.common.models.result.TasksBatchProcessResult

import scala.util.Random

class QueueTaskBalancerSingleton(queueTaskTypedManager: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case RequestTasksBatch(requestId, taskTypes) if taskTypes.nonEmpty =>
      log.debug(s"RequestTasksBatch, requestId: $requestId, types: $taskTypes")

      if (taskTypes.isEmpty) {
        sender() ! EmptyTaskTypeList
      } else if (taskTypes.lengthCompare(1) == 0) {
        val task = taskTypes.head
        queueTaskTypedManager ! QueueTaskController.RequestTask(requestId, task, sender())
      } else {
        val task = Random.shuffle(taskTypes.toIndexedSeq).head
        queueTaskTypedManager ! QueueTaskController.RequestTask(requestId, task, sender())
      }

    case TasksBatchProcessResult(requestId, taskType, successIds, failureIds, newTasks) =>
      log.debug(s"TasksBatchProcessResult, requestId: $requestId, types: $taskType")

      if (successIds.nonEmpty) {
        queueTaskTypedManager ! QueueTaskController.MarkAsProcessed(requestId, taskType, successIds, sender())
      }
      if (failureIds.nonEmpty) {
        queueTaskTypedManager ! QueueTaskController.MarkAsFailed(requestId, taskType, successIds, sender())
      }
      if (newTasks.nonEmpty) {
        newTasks.foreach { case (newTaskType, newTasksData) =>
          queueTaskTypedManager ! QueueTaskController.AddNewTasks(requestId, newTaskType, newTasksData, sender())
        }
      }
  }
}

object QueueTaskBalancerSingleton {
  def props(queueTaskTypedManager: ActorRef): Props = Props(new QueueTaskBalancerSingleton(queueTaskTypedManager))

  def name(): String = "queueTaskBalancer"

  case object Stop
}
