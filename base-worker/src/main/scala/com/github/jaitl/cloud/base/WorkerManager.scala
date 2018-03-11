package com.github.jaitl.cloud.base

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.cloud.base.WorkerManager.RequestBatch
import com.github.jaitl.cloud.base.config.WorkerConfig
import com.github.jaitl.cloud.base.creator.TwoArgumentActorCreator
import com.github.jaitl.cloud.base.executor.TasksBatchController
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.common.models.request.FailureTasksBatchRequest
import com.github.jaitl.cloud.common.models.request.NoTasks
import com.github.jaitl.cloud.common.models.request.RequestTasksBatch
import com.github.jaitl.cloud.common.models.request.SuccessTasksBatchRequest
import com.github.jaitl.cloud.common.models.request.TaskTypeWithBatchSize
import com.github.jaitl.cloud.common.models.task.TasksBatch

private class WorkerManager(
  queueTaskBalancer: ActorRef,
  pipelines: Map[String, Pipeline],
  config: WorkerConfig,
  tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline]
) extends Actor {
  private val taskTypes = pipelines.values.map(pipe => TaskTypeWithBatchSize(pipe.taskType, pipe.batchSize)).toSeq

  override def receive: Receive = {

    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), taskTypes)
      } else {
        scheduleTimeout()
      }

    case SuccessTasksBatchRequest(requestId, taskType, tasksBatch) =>
      self ! RequestBatch

      val tasksBatchController = tasksBatchControllerCreator.create(this.context, tasksBatch, pipelines(taskType))
      tasksBatchController ! TasksBatchController.ExecuteTask

    case FailureTasksBatchRequest(requestId, taskType, throwable) =>
      scheduleTimeout()

    case NoTasks(requestId, taskType) =>
      scheduleTimeout()
  }

  private def scheduleTimeout(): Unit = ???
}

private object WorkerManager {

  case object RequestBatch

  def props(
    queueTaskBalancer: ActorRef,
    pipelines: Map[String, Pipeline],
    config: WorkerConfig,
    tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline]
  ): Props =
    Props(new WorkerManager(queueTaskBalancer, pipelines, config, tasksBatchControllerCreator))

  def name(): String = "workerManager"
}
