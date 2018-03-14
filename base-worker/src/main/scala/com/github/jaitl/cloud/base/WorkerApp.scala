package com.github.jaitl.cloud.base

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.cloud.base.config.WorkerConfig
import com.github.jaitl.cloud.base.creator.PropsActorCreator
import com.github.jaitl.cloud.base.exception.NoPipelinesException
import com.github.jaitl.cloud.base.executor.CrawlExecutor
import com.github.jaitl.cloud.base.executor.SaveCrawlResultController.SaveCrawlResultControllerConfig
import com.github.jaitl.cloud.base.executor.SaveCrawlResultControllerCreator
import com.github.jaitl.cloud.base.executor.TasksBatchController.TasksBatchControllerConfig
import com.github.jaitl.cloud.base.executor.TasksBatchControllerCreator
import com.github.jaitl.cloud.base.executor.resource.ResourceControllerCreator
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.github.jaitl.cloud.base.scheduler.AkkaScheduler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

// scalastyle:off
object WorkerApp extends StrictLogging {
  import scala.concurrent.duration._

  private var pipelines: Option[Map[String, Pipeline]] = None
  private var parallelBatches: Option[Int] = Some(2)

  def addPipelines(pipelines: Seq[Pipeline]): this.type = {
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
      props = CrawlExecutor.props().withDispatcher("blocking-io-dispatcher")
    )

    val resourceControllerCreator = new ResourceControllerCreator()

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

    // TODO read from application.conf
    val workerConfig = WorkerConfig(parallelBatches.get)

    val workerManager = system.actorOf(
      WorkerManager.props(queueTaskBalancer, pipelines.get, workerConfig, tasksBatchControllerCreator),
      WorkerManager.name()
    )

    workerManager ! WorkerManager.RequestBatch
  }
}
