package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.email.BaseNotification

private[worker] case class ConfigurablePipeline(
  batchSize: Int,
  resourceType: ResourceType,
  emailNotification: Boolean,
  emailer: BaseNotification
)
