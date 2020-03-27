package com.github.jaitl.crawler.worker

import java.time.Instant
import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import com.github.jaitl.crawler.master.client.task.TaskTypeWithBatchSize
import com.github.jaitl.crawler.master.client.task.TasksBatch
import com.github.jaitl.crawler.worker.WorkerManager.CheckTimeout
import com.github.jaitl.crawler.worker.WorkerManager.BatchTasksRecived
import com.github.jaitl.crawler.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.worker.WorkerManager.TaskBatchContext
import com.github.jaitl.crawler.worker.client.QueueClient
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.creator.ThreeArgumentActorCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipeline
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.Scheduler
import com.github.jaitl.crawler.worker.validators.BatchTasksValidator

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

private[worker] class WorkerManager(
  queueClient: QueueClient,
  pipelines: Map[String, Pipeline[_]],
  configurablePipeline: ConfigurablePipeline,
  config: WorkerConfig,
  tasksBatchControllerCreator: ThreeArgumentActorCreator[TasksBatch, Pipeline[_], ConfigurablePipeline],
  batchRequestScheduler: Scheduler,
  batchExecutionTimeoutScheduler: Scheduler,
  batchTasksValidator: BatchTasksValidator
)(implicit executionContext: ExecutionContext) extends Actor
    with ActorLogging {
  val batchControllers: mutable.Map[ActorRef, TaskBatchContext] = mutable.Map.empty

  batchRequestScheduler.schedule(config.executeInterval, self, RequestBatch)
  batchExecutionTimeoutScheduler.schedule(config.runExecutionTimeoutCheckInterval, self, CheckTimeout)

  val taskTypes: Seq[TaskTypeWithBatchSize] = pipelines.values
    .map(pipe => TaskTypeWithBatchSize(pipe.taskType, configurablePipeline.batchSize)).toSeq

  override def receive: Receive = monitors.orElse(balancerActions)

  private def balancerActions: Receive = {
    case RequestBatch =>
      if (context.children.size < config.parallelBatches) {
        val id = UUID.randomUUID()
        log.info(s"RequestBatch size: ${context.children.size} params: ${config.parallelBatches}, id: $id")
        queueClient.getTasks(id, taskTypes).onComplete {
          case Success(Some(batch)) =>
            self ! BatchTasksRecived(batch)
          case Success(None) =>
            log.debug("No tasks")
          case Failure(ex) =>
            log.error(ex, "Fail during request tasks")
        }
      }

    case BatchTasksRecived(tasksBatch@TasksBatch(requestId, taskType, _, _)) =>
      log.info(
        s"SuccessTasksBatchRequest queue size: ${context.children.size} " +
          s"batchControllers: ${batchControllers.size}, id: $requestId, items: ${tasksBatch.tasks.size}")
      val newBatch = tasksBatch
        .copy(tasks = tasksBatch.tasks.map(t => t.copy(skipped = batchTasksValidator.validateBatchItem(t))))
      val tasksBatchController =
        tasksBatchControllerCreator.create(this.context, newBatch, pipelines(taskType), configurablePipeline)
      batchControllers += tasksBatchController -> TaskBatchContext(tasksBatchController, Instant.now(), taskType)
      context.watch(tasksBatchController)
      tasksBatchController ! TasksBatchController.ExecuteTask
  }

  private def monitors: Receive = {
    case Terminated(ctrl) =>
      log.error(s"Force stop task batch controller: $ctrl")
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
  case class BatchTasksRecived(tasks: TasksBatch)
  case object CheckTimeout
  case object RequestBatch

  // scalastyle:off parameter.number
  def props(
    queueClient: QueueClient,
    pipelines: Map[String, Pipeline[_]],
    configurablePipeline: ConfigurablePipeline,
    config: WorkerConfig,
    tasksBatchControllerCreator: ThreeArgumentActorCreator[TasksBatch, Pipeline[_], ConfigurablePipeline],
    batchRequestScheduler: Scheduler,
    batchExecutionTimeoutScheduler: Scheduler,
    batchTasksValidator: BatchTasksValidator
  )(implicit executionContext: ExecutionContext): Props =
    Props(
      new WorkerManager(
        queueClient = queueClient,
        pipelines = pipelines,
        configurablePipeline = configurablePipeline,
        config = config,
        tasksBatchControllerCreator = tasksBatchControllerCreator,
        batchRequestScheduler = batchRequestScheduler,
        batchExecutionTimeoutScheduler = batchExecutionTimeoutScheduler,
        batchTasksValidator = batchTasksValidator
      )(executionContext))

  def name(): String = "workerManager"
}
