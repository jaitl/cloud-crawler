package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.notification.BaseNotification
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

private[pipeline] class ConfigurablePipelineBuilder {
  private var batchSize: Option[Int] = None
  private var resourceType: Option[ResourceType] = None
  private var notifier: Option[BaseNotification] = Option.empty
  private var enableNotification: Boolean = false

  def withBatchSize(batchSize: Int): this.type = {
    this.batchSize = Some(batchSize)
    this
  }

  def withProxy(host: String, post: Int, limit: Int, timeout: RandomTimeout): this.type = {
    resourceType = Some(Proxy(host, post, limit, timeout))
    this
  }

  def withEnableNotification(enableNotify: Boolean): this.type = {
    enableNotification = enableNotify
    this
  }

  def withProxy(
    host: String,
    post: Int,
    limit: Int,
    timeout: RandomTimeout,
    login: String,
    password: String): this.type = {
    resourceType = Some(Proxy(host, post, limit, timeout, login, password))
    this
  }

  def withProxy(res: ResourceType): this.type = {
    resourceType = Some(res)
    this
  }

  def withTor(
    host: String,
    post: Int,
    limit: Int,
    timeout: RandomTimeout,
    controlPort: Int,
    password: String): this.type = {
    resourceType = Some(Tor(host, post, limit, timeout, controlPort, password))
    this
  }

  def withNotifier(notificationImpl: BaseNotification): this.type = {
    notifier = Some(notificationImpl)
    this
  }

  def build(): ConfigurablePipeline = {

    if (batchSize.isEmpty) {
      throw new PipelineBuilderException("batch size is not defined")
    }
    if (resourceType.isEmpty) {
      throw new PipelineBuilderException("proxy or tor is not defined")
    }
    if (enableNotification && notifier.isEmpty) {
      throw new PipelineBuilderException("Notifier is not defined")
    }

    ConfigurablePipeline(
      batchSize = batchSize.get,
      resourceType = resourceType.get,
      enableNotification = enableNotification,
      notifier = notifier.orNull
    )
  }
}

object ConfigurablePipelineBuilder {
  def apply(): ConfigurablePipelineBuilder = new ConfigurablePipelineBuilder()
  def noParserPipeline(): ConfigurablePipelineBuilder = new ConfigurablePipelineBuilder()
}
