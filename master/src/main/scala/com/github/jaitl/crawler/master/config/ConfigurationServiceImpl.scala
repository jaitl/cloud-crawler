package com.github.jaitl.crawler.master.config

import com.github.jaitl.crawler.master.client.configuration.ConfigReply
import com.github.jaitl.crawler.master.client.configuration.ConfigRequest
import com.github.jaitl.crawler.master.client.configuration.ConfigurationServiceGrpc
import com.github.jaitl.crawler.master.client.configuration.ProjectConfiguration
import com.github.jaitl.crawler.master.client.configuration.ProxyResource
import com.github.jaitl.crawler.master.client.configuration.ProxyResourceReply
import com.github.jaitl.crawler.master.client.configuration.ResourceRequest
import com.github.jaitl.crawler.master.client.configuration.TorResource
import com.github.jaitl.crawler.master.client.configuration.TorResourceReply
import com.github.jaitl.crawler.master.config.provider.CrawlerConfigurationProvider
import com.typesafe.scalalogging.StrictLogging
import io.grpc.BindableService
import io.grpc.ServerServiceDefinition

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ConfigurationServiceImpl(
  configurationProvider: CrawlerConfigurationProvider
)(implicit executionContext: ExecutionContext)
    extends ConfigurationServiceGrpc.ConfigurationService
    with StrictLogging
    with BindableService {
  override def getConfig(request: ConfigRequest): Future[ConfigReply] = {
    logger.info(s"Request config for: ${request.taskType}, ${request.requestId}")
    configurationProvider
      .getCrawlerProjectConfiguration(request.taskType)
      .map {
        case Nil =>
          logger.info(s"Not found config for: ${request.taskType}, ${request.requestId}")
          ConfigReply(ConfigReply.Status.NOT_FOUND)
        case cfg :: Nil =>
          logger.info(s"Found one config for: ${request.taskType}, ${request.requestId}")
          ConfigReply(ConfigReply.Status.OK, Some(cfg))
        case cfgs: Seq[ProjectConfiguration] =>
          logger.error(s"Found ${cfgs.size} configs for: ${request.taskType}, ${request.requestId}")
          ConfigReply(ConfigReply.Status.FAILED)
      }
      .recover {
        case ex: Throwable =>
          logger.error(s"Failure during request config for: ${request.taskType}, ${request.requestId}", ex)
          ConfigReply(ConfigReply.Status.FAILED)
      }
  }

  override def getProxy(request: ResourceRequest): Future[ProxyResourceReply] = {
    logger.info(s"Request proxy for: ${request.taskType}, ${request.requestId}")
    configurationProvider
      .getCrawlerProxyConfiguration(request.taskType)
      .map {
        case Nil =>
          logger.info(s"Not found proxy for: ${request.taskType}, ${request.requestId}")
          ProxyResourceReply(ProxyResourceReply.Status.NOT_FOUND)
        case proxies: Seq[ProxyResource] =>
          logger.info(s"Found proxy for: ${request.taskType}, ${request.requestId}")
          ProxyResourceReply(ProxyResourceReply.Status.OK, Some(proxies.head))
      }
      .recover {
        case ex: Throwable =>
          logger.error(s"Failure during request proxy for: ${request.taskType}, ${request.requestId}", ex)
          ProxyResourceReply(ProxyResourceReply.Status.FAILED)
      }
  }

  override def getTor(request: ResourceRequest): Future[TorResourceReply] = {
    logger.info(s"Request tor for: ${request.taskType}, ${request.requestId}")
    configurationProvider
      .getCrawlerTorConfiguration(request.taskType)
      .map {
        case Nil =>
          logger.info(s"Not found tor for: ${request.taskType}, ${request.requestId}")
          TorResourceReply(TorResourceReply.Status.NOT_FOUND)
        case tors: Seq[TorResource] =>
          logger.info(s"Found tor for: ${request.taskType}, ${request.requestId}, ${tors.head}")
          TorResourceReply(TorResourceReply.Status.OK, Some(tors.head))
      }
      .recover {
        case ex: Throwable =>
          logger.error(s"Failure during request tor for: ${request.taskType}, ${request.requestId}", ex)
          TorResourceReply(TorResourceReply.Status.FAILED)
      }
  }

  override def bindService(): ServerServiceDefinition = ConfigurationServiceGrpc.bindService(this, executionContext)
}
