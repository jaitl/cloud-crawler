package com.github.jaitl.cloud.base.pipeline

import com.github.jaitl.cloud.base.crawler.BaseCrawlerCreator
import com.github.jaitl.cloud.base.parser.BaseParser
import com.github.jaitl.cloud.base.save.SaveParsedProvider
import com.github.jaitl.cloud.base.save.SaveRawProvider

class PipelineBuilder {
  def withTaskName(piplineName: String): PipelineBuilder = ???

  def withCrawlerCreator(crawlerCreator: BaseCrawlerCreator): PipelineBuilder = ???

  def withParser(parser: BaseParser): PipelineBuilder = ???

  def withSaveResultProvider(saveParsedProvider: SaveParsedProvider): PipelineBuilder = ???

  def withSaveRawProvider(saveRawProvider: SaveRawProvider): PipelineBuilder = ???

  def build(): Pipeline = ???
}

object PipelineBuilder {
  def apply(): PipelineBuilder = new PipelineBuilder()
}
