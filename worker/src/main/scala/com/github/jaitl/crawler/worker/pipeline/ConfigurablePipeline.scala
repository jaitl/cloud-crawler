package com.github.jaitl.crawler.worker.pipeline

private[worker] case class ConfigurablePipeline[T](
  batchSize: Int,
  resourceType: ResourceType
)