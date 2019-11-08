package com.github.jaitl.crawler.worker

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
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.WorkerManager.EmptyList
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureConfigRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.FailureProxyRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.NoConfigs
import com.github.jaitl.crawler.models.worker.WorkerManager.NoProxies
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestResource
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessProxyRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTasksConfigRequest
import com.github.jaitl.crawler.models.worker.WorkerManager.SuccessTorRequest
import com.github.jaitl.crawler.worker.WorkerManager.CheckTimeout
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.creator.PropsActorCreator
import com.github.jaitl.crawler.worker.notification.NotificationExecutor
import com.github.jaitl.crawler.worker.executor.CrawlExecutor
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultControllerCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchControllerCreator
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerCreator
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipeline
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.AkkaScheduler
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

// scalastyle:off
private[worker] class WarmUpManager(
  pipeline: Pipeline[_],
  system: ActorSystem,
  configurationBalancer: ActorRef,
  resourceBalancer: ActorRef,
  config: Config
) extends Actor
    with ActorLogging {

  override def receive: Receive = monitors.orElse(balancerActions)

  private def balancerActions: Receive = {
    case RequestConfiguration => {
      configurationBalancer ! RequestConfiguration(UUID.randomUUID(), pipeline.taskType)
    }
    case SuccessTorRequest(requestId, taskType, tor) => {}
    case SuccessTasksConfigRequest(requestId, taskType, configuration) => {
      log.info(s"Config received: $configuration")
      resourceBalancer ! RequestResource(UUID.randomUUID(), configuration)
    }
    case FailureConfigRequest(requestId, taskType, throwable) => {
      log.info(s"Exception for $taskType received! $throwable")
    }
    case NoConfigs(requestId, taskType) => {
      log.info(s"No config for $taskType received!")
    }
    case SuccessProxyRequest(requestId, configuration, proxy) => {
      log.info(s"Proxy for ${configuration.workerTaskType} received! $proxy")
      requestBatch(
        config,
        configuration,
        ConfigurablePipelineBuilder()
          .withBatchSize(configuration.workerBatchSize)
          .withProxy(
            proxy.workerProxyHost,
            proxy.workerProxyPort,
            proxy.workerParallel,
            RandomTimeout(Duration(proxy.workerProxyTimeoutUp), Duration(proxy.workerProxyTimeoutDown)),
            proxy.workerProxyLogin,
            proxy.workerProxyPassword
          )
          .withNotifier(pipeline.notifier.get)
          .withEnableNotification(configuration.notification)
          .build()
      )
    }
    case NoProxies(requestId, taskType) => {
      log.info(s"No proxy for ${taskType.workerTaskType} received!")
    }
    case FailureProxyRequest(requestId, taskType, throwable) => {
      log.info(s"Exception for $taskType received! $throwable")
    }
  }

  private def monitors: Receive = {
    case Terminated(ctrl) =>
      log.error(s"Force stop task batch controller: $ctrl")

    case CheckTimeout =>
      val now = Instant.now()
  }

  def requestBatch(
    config: Config,
    configuration: ProjectConfiguration,
    configurablePipeline: ConfigurablePipeline): Unit = {

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

    val notifierExecutorCreator = new PropsActorCreator(
      actorName = NotificationExecutor.name(),
      props = NotificationExecutor.props().withDispatcher("worker.blocking-io-dispatcher")
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
      notifierExecutorCreator = notifierExecutorCreator,
      saveCrawlResultCreator = saveCrawlResultControllerCreator,
      queueTaskBalancer = queueTaskBalancer,
      executeScheduler = new AkkaScheduler(system),
      config = tasksBatchControllerConfig
    )

    val workerManager = system.actorOf(
      WorkerManager.props(
        queueTaskBalancer = queueTaskBalancer,
        pipelines = Map(pipeline.taskType -> pipeline),
        configurablePipeline = configurablePipeline,
        config = workerConfig,
        tasksBatchControllerCreator = tasksBatchControllerCreator,
        batchRequestScheduler = new AkkaScheduler(system),
        batchExecutionTimeoutScheduler = new AkkaScheduler(system)
      ),
      WorkerManager.name()
    )
    workerManager ! RequestBatch
  }
}

private[worker] object WarmUpManager {
  case object CheckTimeout

  def props(
    pipeline: Pipeline[_],
    system: ActorSystem,
    configurationBalancer: ActorRef,
    resourceBalancer: ActorRef,
    config: Config
  ): Props =
    Props(
      new WarmUpManager(
        pipeline,
        system,
        configurationBalancer,
        resourceBalancer,
        config
      ))

  def name(): String = "warmUpManager"
}
