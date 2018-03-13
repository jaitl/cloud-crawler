package com.github.jaitl.cloud.base

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.cloud.base.config.WorkerConfig
import com.github.jaitl.cloud.base.creator.PropsActorCreator
import com.github.jaitl.cloud.base.exception.NoPipelinesException
import com.github.jaitl.cloud.base.executor.CrawlExecutor
import com.github.jaitl.cloud.base.executor.TasksBatchControllerCreator
import com.github.jaitl.cloud.base.executor.resource.ResourceControllerCreator
import com.github.jaitl.cloud.base.pipeline.Pipeline
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object WorkerApp extends StrictLogging {

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

    val tasksBatchControllerCreator = new TasksBatchControllerCreator(
      resourceControllerCreator = resourceControllerCreator,
      crawlExecutorCreator = crawlExecutorCreator
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
