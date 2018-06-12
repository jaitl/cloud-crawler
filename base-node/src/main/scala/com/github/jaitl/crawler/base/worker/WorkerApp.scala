package com.github.jaitl.crawler.base.worker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.crawler.base.worker.config.WorkerConfig
import com.github.jaitl.crawler.base.worker.creator.PropsActorCreator
import com.github.jaitl.crawler.base.worker.exception.NoPipelinesException
import com.github.jaitl.crawler.base.worker.executor.CrawlExecutor
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.base.worker.executor.SaveCrawlResultControllerCreator
import com.github.jaitl.crawler.base.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.base.worker.executor.TasksBatchControllerCreator
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceControllerConfig
import com.github.jaitl.crawler.base.worker.executor.resource.ResourceControllerCreator
import com.github.jaitl.crawler.base.worker.pipeline.Pipeline
import com.github.jaitl.crawler.base.worker.scheduler.AkkaScheduler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

// scalastyle:off
class WorkerApp(pipelines: Map[String, Pipeline[_]], parallelBatches: Int, system: ActorSystem) extends StrictLogging {
  import scala.concurrent.duration._

  def run(): Unit = {
    logger.info(s"count pipelines: ${pipelines.size}")

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

    val resourceControllerConfig = ResourceControllerConfig(maxFailCount = 100)

    val resourceControllerCreator = new ResourceControllerCreator(resourceControllerConfig)

    val saveCrawlResultControllerCreator = new SaveCrawlResultControllerCreator(
      queueTaskBalancer = queueTaskBalancer,
      saveScheduler = new AkkaScheduler(system),
      config = SaveCrawlResultControllerConfig(5.minutes) // TODO read from config file
    )

    val tasksBatchControllerCreator = new TasksBatchControllerCreator(
      resourceControllerCreator = resourceControllerCreator,
      crawlExecutorCreator = crawlExecutorCreator,
      saveCrawlResultCreator = saveCrawlResultControllerCreator,
      executeScheduler = new AkkaScheduler(system),
      config = TasksBatchControllerConfig(10, 30.seconds) // TODO read from config file
    )

    val workerConfig = WorkerConfig(parallelBatches, 10.seconds)

    val workerManager = system.actorOf(
      WorkerManager.props(
        queueTaskBalancer = queueTaskBalancer,
        pipelines = pipelines,
        config = workerConfig,
        tasksBatchControllerCreator = tasksBatchControllerCreator,
        batchRequestScheduler = new AkkaScheduler(system)
      ),
      WorkerManager.name()
    )

    workerManager ! WorkerManager.RequestBatch
  }
}
