package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.CrawlerTor

import scala.concurrent.Future

trait CrawlerConfigurationProvider {

  def getCrawlerProxyConfiguration(taskType: String): Future[Seq[CrawlerProxy]]

  def getCrawlerTorConfiguration(taskType: String): Future[Seq[CrawlerTor]]

  def getCrawlerProjectConfiguration(taskType: String): Future[Seq[ProjectConfiguration]]
}
