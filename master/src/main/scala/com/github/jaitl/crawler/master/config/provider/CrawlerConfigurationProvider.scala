package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.models.worker.{ProjectConfiguration, CrawlerProxy, CrawlerTor}

import scala.concurrent.{ExecutionContext, Future}

trait CrawlerConfigurationProvider {

  def getCrawlerProxyConfiguration(taskType: String): Future[Option[CrawlerProxy]]

  def getCrawlerTOrConfiguration(taskType: String): Future[Option[CrawlerTor]]

  def getCrawlerProjectConfiguration(taskType: String)(
    implicit ec: ExecutionContext): Future[Option[ProjectConfiguration]]
}
