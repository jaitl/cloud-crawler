package com.github.jaitl.cloud.base.pipeline

import com.github.jaitl.cloud.base.crawler.BaseCrawler
import com.github.jaitl.cloud.base.parser.BaseParser
import com.github.jaitl.cloud.base.save.SaveProvider

class PipelineBuilder {
  def withCrawler(crawler: BaseCrawler): PipelineBuilder = ???
  def withParser(parser: BaseParser): PipelineBuilder = ???
  def withSaveProvider(saveProvider: SaveProvider): PipelineBuilder = ???
  def build(): Pipeline = ???
}
