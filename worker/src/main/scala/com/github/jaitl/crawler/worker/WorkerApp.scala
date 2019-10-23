package com.github.jaitl.crawler.worker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration
import com.github.jaitl.crawler.worker.pipeline.WarmUpPipeline
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

// scalastyle:off
object WorkerApp extends StrictLogging {

  private var warmUpPipelines: Seq[WarmUpPipeline[_]] = Seq.empty

  def addWarmUpPipeline(pipelines: Seq[WarmUpPipeline[_]]): this.type = {
    this.warmUpPipelines = pipelines
    this
  }

  def run(): Unit = {
    val config = ConfigFactory.load("worker.conf")

    logger.info(
      "Start worker on {}:{}",
      config.getConfig("akka.remote.netty.tcp").getString("hostname"),
      config.getConfig("akka.remote.netty.tcp").getString("port")
    )

    val system = ActorSystem(config.getConfig("clustering.cluster").getString("name"), config)

    val configurationBalancer: ActorRef = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/configurationBalancer",
        settings = ClusterSingletonProxySettings(system).withRole("master")
      ),
      name = "configurationBalancerProxy"
    )

    val warmUpManager = system.actorOf(
      WarmUpManager.props(
        warmUpPipelines,
        system,
        configurationBalancer,
        config
      ),
      WorkerManager.name()
    )
    logger.info("Aktor call")
    warmUpManager ! RequestConfiguration
  }
}
