package com.github.jaitl.crawler.worker

import java.util.UUID

import com.github.jaitl.crawler.master.client.configuration.ConfigRequest
import com.github.jaitl.crawler.master.client.configuration.ConfigurationServiceGrpc
import com.github.jaitl.crawler.master.client.configuration.ProjectConfiguration
import com.github.jaitl.crawler.master.client.configuration.ResourceRequest
import com.github.jaitl.crawler.worker.notification.DummyNotification
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipeline
import com.github.jaitl.crawler.worker.pipeline.ConfigurablePipelineBuilder
import com.github.jaitl.crawler.worker.pipeline.Pipeline
import com.github.jaitl.crawler.worker.timeout.RandomTimeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

private[worker] class StartUpService(
  configurationClient: ConfigurationServiceGrpc.ConfigurationServiceStub,
  pipeline: Pipeline[_],
)(implicit executionContext: ExecutionContext)
    extends StrictLogging {
  def configurePipeline(taskType: String): Future[(ProjectConfiguration, ConfigurablePipeline)] =
    for {
      config <- configurationClient.getConfig(ConfigRequest(UUID.randomUUID().toString, taskType))
      pipeline <- config.config match {
        case Some(cfg@ProjectConfiguration(_, _, _, _, _, _, _, "Tor", _, _)) => configureTor(taskType, cfg)
        case Some(cfg@ProjectConfiguration(_, _, _, _, _, _, _, "Proxy", _, _)) => configureProxy(taskType, cfg)
        case Some(pc) => Future.failed(new Exception(s"Configuration failed, unknown resource: ${pc.workerResource}"))
        case None => Future.failed(new Exception(s"Configuration failed: ${config.status}"))
      }
    } yield (config.config.get, pipeline)

  private def configureTor(taskType: String, configuration: ProjectConfiguration): Future[ConfigurablePipeline] =
    for {
      torCfg <- configurationClient.getTor(ResourceRequest(UUID.randomUUID().toString, taskType))
      pipe = torCfg.tor match {
        case Some(tor) =>
          logger.info(s"Configure tor pipeline for: $taskType")
          ConfigurablePipelineBuilder()
            .withBatchSize(configuration.workerBatchSize)
            .withTor(
              tor.workerTorHost,
              tor.workerTorPort,
              tor.workerTorLimit,
              RandomTimeout(Duration(tor.workerTorTimeoutUp), Duration(tor.workerTorTimeoutDown)),
              tor.workerTorControlPort,
              tor.workerTorPassword
            )
            .withNotifier(pipeline.notifier.getOrElse(new DummyNotification()))
            .withEnableNotification(configuration.notification)
            .build()
        case None => throw new Exception(s"Tor configuration failed: ${torCfg.status}")
      }
    } yield pipe

  private def configureProxy(taskType: String, configuration: ProjectConfiguration): Future[ConfigurablePipeline] =
    for {
      proxyCfg <- configurationClient.getProxy(ResourceRequest(UUID.randomUUID().toString, taskType))
      pipe = proxyCfg.proxy match {
        case Some(proxy) =>
          logger.info(s"Configure proxy pipeline for: $taskType")
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
            .withNotifier(pipeline.notifier.getOrElse(new DummyNotification()))
            .withEnableNotification(configuration.notification)
            .build()
        case None => throw new Exception(s"Proxy configuration failed: ${proxyCfg.status}")
      }
    } yield pipe
}
