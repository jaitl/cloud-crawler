package com.github.jaitl.crawler.worker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import com.github.jaitl.crawler.models.worker.WorkerManager.RequestConfiguration
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

// scalastyle:off
object WorkerApp extends StrictLogging {

  private var warmUpPipelines: Pipeline[_] = null

  def addWarmUpPipeline(pipelines: Pipeline[_]): this.type = {
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

    val resourceBalancer: ActorRef = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/resourceBalancer",
        settings = ClusterSingletonProxySettings(system).withRole("master")
      ),
      name = "resourceBalancer"
    )

    val warmUpManager = system.actorOf(
      WarmUpManager.props(
        warmUpPipelines,
        system,
        configurationBalancer,
        resourceBalancer,
        config
      ),
      WarmUpManager.name()
    )
    logger.info("Aktor call")
    warmUpManager ! RequestConfiguration
  }
}
