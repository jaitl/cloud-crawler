package com.github.jaitl.crawler.worker

import java.time.Duration
import java.time.Instant
import java.util.UUID

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Terminated
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.crawler.models.worker.WorkerManager.EmptyList
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureConfigRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoConfigs
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksConfigRequest
import com.github.jaitl.crawler.worker.WorkerManager.CheckTimeout
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.creator.PropsActorCreator
import com.github.jaitl.crawler.worker.exception.NoPipelinesException
import com.github.jaitl.crawler.worker.executor.CrawlExecutor
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultControllerCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchControllerCreator
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerCreator
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.pipeline.PipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.WarmUpPipeline
import com.github.jaitl.crawler.worker.scheduler.AkkaScheduler
import com.typesafe.config.Config

import scala.reflect.runtime.universe._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import scala.concurrent.duration.FiniteDuration
// scalastyle:off
private[worker] class WarmUpManager(
  pipelines: Seq[WarmUpPipeline[_]],
  system: ActorSystem,
  configurationBalancer: ActorRef,
  config: Config
) extends Actor
    with ActorLogging {
  private var pp: Option[Map[String, Pipeline[_]]] = None

  override def receive: Receive = monitors.orElse(balancerActions)

  private def balancerActions: Receive = {
    case RequestConfiguration => {
      configurationBalancer ! RequestConfiguration(UUID.randomUUID(), pipelines.head.taskType)
    }
    case SuccessTasksConfigRequest(requestId, taskType, configuration) => {
      log.info(s"Config received: $configuration")


      val pipe = PipelineBuilder()
        .withTaskType(taskType)
        .withBatchSize(configuration.workerBatchSize)
        .withCrawler(mirror.staticModule(configuration.workerCrawler).asInstanceOf[BaseCrawler])
        .withParser(mirror.staticModule(configuration.workerParser).asInstanceOf[BaseParser[Any]])
        .build()
      val listOfPipeLines = pipe :: Nil
      pp = Some(listOfPipeLines.map(pipe => pipe.taskType -> pipe).toMap)

      val workerConfig = WorkerConfig(
        configuration.workerParallelBatches,
        config.as[FiniteDuration]("worker.manager.executeInterval"),
        config.as[FiniteDuration]("worker.manager.runExecutionTimeoutCheckInterval"),
        config.as[FiniteDuration]("worker.manager.batchExecutionTimeout")
      )
      val queueTaskBalancer: ActorRef = system.actorOf(
        ClusterSingletonProxy.props(
          singletonManagerPath = "/user/queueTaskBalancer",
          settings = ClusterSingletonProxySettings(system).withRole("master")
        ),
        name = "queueTaskBalancerProxy"
      )
      val crawlExecutorCreator = new PropsActorCreator(
        actorName = CrawlExecutor.name(),
        props = CrawlExecutor.props().withDispatcher("worker.blocking-io-dispatcher")
      )
      val saveCrawlResultControllerConfig = config.as[SaveCrawlResultControllerConfig]("worker.save-controller")

      val saveCrawlResultControllerCreator = new SaveCrawlResultControllerCreator(
        queueTaskBalancer = queueTaskBalancer,
        saveScheduler = new AkkaScheduler(system),
        config = saveCrawlResultControllerConfig
      )

      val resourceControllerConfig = config.as[ResourceControllerConfig]("worker.resource-controller")
      val tasksBatchControllerConfig = config.as[TasksBatchControllerConfig]("worker.task-batch-controller")

      val resourceControllerCreator = new ResourceControllerCreator(resourceControllerConfig)

      val tasksBatchControllerCreator = new TasksBatchControllerCreator(
        resourceControllerCreator = resourceControllerCreator,
        crawlExecutorCreator = crawlExecutorCreator,
        saveCrawlResultCreator = saveCrawlResultControllerCreator,
        queueTaskBalancer = queueTaskBalancer,
        executeScheduler = new AkkaScheduler(system),
        config = tasksBatchControllerConfig
      )

      pp match {
        case Some(pip) if pip.nonEmpty => log.info(s"count pipelines: ${pip.size}")
        case _ => throw new NoPipelinesException
      }
      val workerManager = system.actorOf(
        WorkerManager.props(
          queueTaskBalancer = queueTaskBalancer,
          pipelines = pp.get,
          config = workerConfig,
          tasksBatchControllerCreator = tasksBatchControllerCreator,
          batchRequestScheduler = new AkkaScheduler(system),
          batchExecutionTimeoutScheduler = new AkkaScheduler(system)
        ),
        WorkerManager.name()
      )
      workerManager ! RequestBatch
    }
    case FailureConfigRequest(requestId, taskType, throwable) => {
      log.info(s"Exception for $taskType received! $throwable")
    }
    case NoConfigs(requestId, taskType) => {
      log.info(s"No config for $taskType received!")
    }
    case EmptyList =>
  }

  private def monitors: Receive = {
    case Terminated(ctrl) =>
      log.error(s"Force stop task batch controller: $ctrl")

    case CheckTimeout =>
      val now = Instant.now()
  }
}

private[worker] object WarmUpManager {
  case object CheckTimeout

  def props(
    pipelines: Seq[WarmUpPipeline[_]],
    system: ActorSystem,
    configurationBalancer: ActorRef,
    config: Config
  ): Props =
    Props(
      new WarmUpManager(
        pipelines,
        system,
        configurationBalancer,
        config
      ))

  def name(): String = "warmUpManager"
}
