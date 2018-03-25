package com.github.jaitl.crawler.base.worker

import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.github.jaitl.crawler.base.models.task.TasksBatch
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.RequestTasksBatch
import com.github.jaitl.crawler.base.master.queue.QueueTaskBalancer.TaskTypeWithBatchSize
import com.github.jaitl.crawler.base.worker.WorkerManager.EmptyTaskTypeList
import com.github.jaitl.crawler.base.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.base.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.base.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.base.worker.WorkerManager.SuccessTasksBatchRequest
import com.github.jaitl.crawler.base.worker.config.WorkerConfig
import com.github.jaitl.crawler.base.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline

private[base] class WorkerManager(
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

    case EmptyTaskTypeList =>
  }

  private def scheduleTimeout(): Unit = ???
}

private[base] object WorkerManager {

  case object RequestBatch

  case class SuccessTasksBatchRequest(
    requestId: UUID,
    taskType: String,
    tasksBatch: TasksBatch
  )

  case class FailureTasksBatchRequest(
    requestId: UUID,
    taskType: String,
    throwable: Throwable
  )

  case class NoTasks(requestId: UUID, taskType: String)

  case object EmptyTaskTypeList

  def props(
    queueTaskBalancer: ActorRef,
    pipelines: Map[String, Pipeline],
    config: WorkerConfig,
    tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline]
  ): Props =
    Props(new WorkerManager(queueTaskBalancer, pipelines, config, tasksBatchControllerCreator))

  def name(): String = "workerManager"
}
