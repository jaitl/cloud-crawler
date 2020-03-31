package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.master.client.configuration.ProjectConfiguration
import com.github.jaitl.crawler.master.client.configuration.ProxyResource
import com.github.jaitl.crawler.master.client.configuration.TorResource

import scala.concurrent.Future

trait CrawlerConfigurationProvider {

  def getCrawlerProxyConfiguration(taskType: String): Future[Seq[ProxyResource]]

  def getCrawlerTorConfiguration(taskType: String): Future[Seq[TorResource]]

  def getCrawlerProjectConfiguration(taskType: String): Future[Seq[ProjectConfiguration]]
}
