package com.github.jaitl.crawler.base.master.queue

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.RequestTasksBatch
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.TasksBatchProcessResult
import com.github.jaitl.crawler.base.worker.WorkerManager.EmptyTaskTypeList


import scala.util.Random

class QueueTaskBalancer(
  queueTaskQueueReqCtrl: ActorRef,
  queueTaskQueueResCtrl: ActorRef
) extends Actor with ActorLogging {
  override def receive: Receive = {
    case RequestTasksBatch(requestId, taskTypes) if taskTypes.nonEmpty =>
      log.debug(s"RequestTasksBatch, requestId: $requestId, types: $taskTypes")

      if (taskTypes.isEmpty) {
        sender() ! EmptyTaskTypeList
      } else if (taskTypes.lengthCompare(1) == 0) {
        val task = taskTypes.head
        queueTaskQueueReqCtrl ! QueueTaskRequestController.RequestTask(requestId, task.taskType, task.batchSize, sender())
      } else {
        val task = Random.shuffle(taskTypes.toIndexedSeq).head
        queueTaskQueueReqCtrl ! QueueTaskRequestController.RequestTask(requestId, task.taskType, task.batchSize, sender())
      }

    case TasksBatchProcessResult(requestId, taskType, successIds, failureIds, newTasks) =>
      log.debug(s"TasksBatchProcessResult, requestId: $requestId, types: $taskType")

      if (successIds.nonEmpty) {
        queueTaskQueueResCtrl ! QueueTaskResultController.MarkAsProcessed(requestId, taskType, successIds, sender())
      }
      if (failureIds.nonEmpty) {
        queueTaskQueueResCtrl ! QueueTaskResultController.MarkAsFailed(requestId, taskType, failureIds, sender())
      }
      if (newTasks.nonEmpty) {
        newTasks.foreach { case (newTaskType, newTasksData) =>
          queueTaskQueueResCtrl ! QueueTaskResultController.AddNewTasks(requestId, newTaskType, newTasksData, sender())
        }
      }
  }
}

object QueueTaskBalancer {
  case class RequestTasksBatch(requestId: UUID, taskTypes: Seq[TaskTypeWithBatchSize])

  case class TaskTypeWithBatchSize(taskType: String, batchSize: Int)

  case class TasksBatchProcessResult(
    requestId: UUID,
    taskType: String,
    successIds: Seq[String],
    failureIds: Seq[String],
    newTasks: Map[String, Seq[String]]
  )

  def props(queueTaskQueueReqCtrl: ActorRef, queueTaskQueueResCtrl: ActorRef): Props =
    Props(new QueueTaskBalancer(queueTaskQueueReqCtrl, queueTaskQueueResCtrl))

  def name(): String = "queueTaskBalancer"

  case object Stop
}
