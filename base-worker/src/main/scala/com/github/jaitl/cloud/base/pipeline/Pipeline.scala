package com.github.jaitl.cloud.base.pipeline

import com.github.jaitl.cloud.base.crawler.BaseCrawlerCreator
import com.github.jaitl.cloud.base.parser.BaseParser
import com.github.jaitl.cloud.base.save.SaveParsedProvider
import com.github.jaitl.cloud.base.save.SaveRawProvider

case class Pipeline(
  taskType: String,
  batchSize: Int,
  crawlerCreator: BaseCrawlerCreator,
  saveRawProvider: Option[SaveRawProvider],
  parser: Option[BaseParser],
  saveParsedProvider: Option[SaveParsedProvider],
  resourceType: ResourceType
)
