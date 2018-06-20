package com.github.jaitl.crawler.worker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestBatch
import com.github.jaitl.crawler.worker.config.WorkerConfig
import com.github.jaitl.crawler.worker.creator.PropsActorCreator
import com.github.jaitl.crawler.worker.exception.NoPipelinesException
import com.github.jaitl.crawler.worker.executor.CrawlExecutor
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.crawler.worker.executor.SaveCrawlResultControllerCreator
import com.github.jaitl.crawler.worker.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.crawler.worker.executor.TasksBatchControllerCreator
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerConfig
import com.github.jaitl.crawler.worker.executor.resource.ResourceControllerCreator
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.scheduler.AkkaScheduler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

// scalastyle:off
object WorkerApp extends StrictLogging {
  import scala.concurrent.duration._

  private var pipelines: Option[Map[String, Pipeline[_]]] = None
  private var parallelBatches: Option[Int] = Some(2)

  def addPipelines(pipelines: Seq[Pipeline[_]]): this.type = {
    this.pipelines = Some(pipelines.map(pipe => pipe.taskType -> pipe).toMap)
    this
  }

  def parallelBatches(limitParallelBatches: Int): this.type = {
    parallelBatches = Some(limitParallelBatches)
    this
  }

  def run(): Unit = {
    pipelines match {
      case Some(pip) if pip.nonEmpty => logger.info(s"count pipelines: ${pip.size}")
      case _ => throw new NoPipelinesException
    }

    val config = ConfigFactory.load("worker.conf")

    logger.info(
      "Start worker on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem("cloudCrawlerSystem", config)

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

    val resourceControllerConfig = ResourceControllerConfig(maxFailCount = 3)

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
      config = TasksBatchControllerConfig(10, 100.millis) // TODO read from config file
    )

    val workerConfig = WorkerConfig(parallelBatches.get, 1.seconds) // TODO read from config file

    val workerManager = system.actorOf(
      WorkerManager.props(
        queueTaskBalancer = queueTaskBalancer,
        pipelines = pipelines.get,
        config = workerConfig,
        tasksBatchControllerCreator = tasksBatchControllerCreator,
        batchRequestScheduler = new AkkaScheduler(system)
      ),
      WorkerManager.name()
    )

    workerManager ! RequestBatch
  }
}
