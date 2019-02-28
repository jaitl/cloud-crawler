package com.github.jaitl.crawler.worker

import java.time.Instant
import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import com.github.jaitl.crawler.models.task.TasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.EmptyTaskTypeList
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoTasks
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestTasksBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksBatchRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.TaskTypeWithBatchSize
import com.github.jaitl.crawler.worker.WorkerManager.CheckTimeout
import com.github.jaitl.crawler.worker.WorkerManager.TaskBatchContext
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.creator.TwoArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.Scheduler
import com.typesafe.scalalogging.StrictLogging

import scala.collection.mutable

private[worker] class WorkerManager(
  queueTaskBalancer: ActorRef,
  pipelines: Map[String, Pipeline[_]],
  config: WorkerConfig,
  tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline[_]],
  batchRequestScheduler: Scheduler,
  batchExecutionTimeoutScheduler: Scheduler
) extends Actor with ActorLogging {
  val taskTypes = pipelines.values.map(pipe => TaskTypeWithBatchSize(pipe.taskType, pipe.batchSize)).toSeq
  val batchControllers: mutable.Map[ActorRef, TaskBatchContext] = mutable.Map.empty

  batchRequestScheduler.schedule(config.executeInterval, self, RequestBatch)
  batchExecutionTimeoutScheduler.schedule(config.runExecutionTimeoutCheckInterval, self, CheckTimeout)

  override def receive: Receive = monitors orElse balancerActions

  private def balancerActions: Receive = {
    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        log.info(s"Children size: $context.children.size params: $config.parallelBatches")
        queueTaskBalancer ! RequestTasksBatch(UUID.randomUUID(), taskTypes)
      }

    case SuccessTasksBatchRequest(requestId, taskType, tasksBatch) =>
      val tasksBatchController = tasksBatchControllerCreator.create(this.context, tasksBatch, pipelines(taskType))
      batchControllers += tasksBatchController -> TaskBatchContext(tasksBatchController, Instant.now(), taskType)
      context.watch(tasksBatchController)
      tasksBatchController ! TasksBatchController.ExecuteTask

    case FailureTasksBatchRequest(requestId, taskType, throwable) =>

    case NoTasks(requestId, taskType) =>

    case EmptyTaskTypeList =>
  }

  private def monitors: Receive = {
    case Terminated(ctrl) =>
      batchControllers -= ctrl

    case CheckTimeout =>
      val now = Instant.now()

      val overdue = batchControllers.values
        .filter(c => now.isAfter(c.startTime.plusMillis(config.batchExecutionTimeout.toMillis)))

      overdue.foreach { c =>
        log.error(s"Force stop task batch controller: ${c.ctrl}, taskType: ${c.taskType}")
        context.stop(c.ctrl)
        batchControllers -= c.ctrl
      }
  }
}

private[worker] object WorkerManager {
  case class TaskBatchContext(ctrl: ActorRef, startTime: Instant, taskType: String)
  case object CheckTimeout

  def props(
    queueTaskBalancer: ActorRef,
    pipelines: Map[String, Pipeline[_]],
    config: WorkerConfig,
    tasksBatchControllerCreator: TwoArgumentActorCreator[TasksBatch, Pipeline[_]],
    batchRequestScheduler: Scheduler,
    batchExecutionTimeoutScheduler: Scheduler
  ): Props =
    Props(new WorkerManager(
      queueTaskBalancer,
      pipelines,
      config,
      tasksBatchControllerCreator,
      batchRequestScheduler,
      batchExecutionTimeoutScheduler
    ))

  def name(): String = "workerManager"
}
