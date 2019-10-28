package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.NoParser
import com.github.jaitl.crawler.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

private[pipeline] class PipelineBuilder[T] {
  private var taskType: Option[String] = None
  private var crawler: Option[BaseCrawler] = None
  private var saveRawProvider: Option[SaveRawProvider] = None
  private var parser: Option[BaseParser[T]] = None
  private var saveParsedProvider: Option[SaveParsedProvider[T]] = None

  def withTaskType(taskType: String): this.type = {
    this.taskType = Some(taskType)
    this
  }

  def withCrawler(crawler: BaseCrawler): this.type = {
    this.crawler = Some(crawler)
    this
  }

  def withSaveRawProvider(saveRawProvider: SaveRawProvider): this.type = {
    this.saveRawProvider = Some(saveRawProvider)
    this
  }

  def withParser(parser: BaseParser[T]): this.type = {
    this.parser = Some(parser)
    this
  }

  def withSaveResultProvider(saveParsedProvider: SaveParsedProvider[T]): this.type = {
    this.saveParsedProvider = Some(saveParsedProvider)
    this
  }

  def build(): Pipeline[T] = {
    if (taskType.isEmpty) {
      throw new PipelineBuilderException("task type is not defined")
    }

    if (crawler.isEmpty) {
      throw new PipelineBuilderException("crawler is not defined")
    }

    (parser, saveParsedProvider) match {
      case (Some(_), Some(_)) | (None, None) =>
      case _ => throw new PipelineBuilderException("parser or saveParsedProvider is not defined")
    }

    Pipeline(
      taskType = taskType.get,
      crawler = crawler.get,
      saveRawProvider = saveRawProvider,
      parser = parser,
      saveParsedProvider = saveParsedProvider
    )
  }
}

object PipelineBuilder {
  def apply[T](): PipelineBuilder[T] = new PipelineBuilder[T]()
  def noParserPipeline(): PipelineBuilder[NoParser] = new PipelineBuilder[NoParser]()
}

class PipelineBuilderException(message: String) extends Exception(message)
