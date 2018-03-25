package com.github.jaitl.crawler.base.worker.pipeline

import com.github.jaitl.crawler.base.worker.crawler.BaseCrawlerCreator
import com.github.jaitl.crawler.base.worker.parser.BaseParser
import com.github.jaitl.crawler.base.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.base.worker.save.SaveRawProvider

case class Pipeline(
  taskType: String,
  batchSize: Int,
  crawlerCreator: BaseCrawlerCreator,
  saveRawProvider: Option[SaveRawProvider],
  parser: Option[BaseParser],
  saveParsedProvider: Option[SaveParsedProvider],
  resourceType: ResourceType
)
