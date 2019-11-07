package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.notification.BaseNotification

private[worker] case class ConfigurablePipeline(
  batchSize: Int,
  resourceType: ResourceType,
  enableNotification: Boolean,
  notifier: BaseNotification
)
