package com.github.jaitl.crawler.worker.pipeline

private[worker] case class ConfigurablePipeline(
  batchSize: Int,
  resourceType: ResourceType
)
