package com.github.jaitl.crawler.base.worker.pipeline

import com.github.jaitl.crawler.base.worker.crawler.BaseCrawlerCreator
import com.github.jaitl.crawler.base.worker.parser.BaseParser
import com.github.jaitl.crawler.base.worker.parser.ParsedData
import com.github.jaitl.crawler.base.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.base.worker.save.SaveRawProvider

private[base] case class Pipeline[T <: ParsedData](
  taskType: String,
  batchSize: Int,
  crawlerCreator: BaseCrawlerCreator,
  saveRawProvider: Option[SaveRawProvider],
  parser: Option[BaseParser[T]],
  saveParsedProvider: Option[SaveParsedProvider[T]],
  resourceType: ResourceType
)
