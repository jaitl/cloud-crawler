package com.github.jaitl.cloud.base.pipeline

import com.github.jaitl.cloud.base.crawler.BaseCrawlerCreator
import com.github.jaitl.cloud.base.parser.BaseParser
import com.github.jaitl.cloud.base.save.SaveParsedProvider
import com.github.jaitl.cloud.base.save.SaveRawProvider

class PipelineBuilder {
  private var taskType: Option[String] = None
  private var batchSize: Option[Int] = None
  private var crawlerCreator: Option[BaseCrawlerCreator] = None
  private var saveRawProvider: Option[SaveRawProvider] = None
  private var parser: Option[BaseParser] = None
  private var saveParsedProvider: Option[SaveParsedProvider] = None
  private var resourceType: Option[ResourceType] = None

  def withTaskType(taskType: String): PipelineBuilder = {
    this.taskType = Some(taskType)
    this
  }

  def withBatchSize(batchSize: Int): PipelineBuilder = {
    this.batchSize = Some(batchSize)
    this
  }

  def withCrawlerCreator(crawlerCreator: BaseCrawlerCreator): PipelineBuilder = {
    this.crawlerCreator = Some(crawlerCreator)
    this
  }

  def withSaveRawProvider(saveRawProvider: SaveRawProvider): PipelineBuilder = {
    this.saveRawProvider = Some(saveRawProvider)
    this
  }


  def withParser(parser: BaseParser): PipelineBuilder = {
    this.parser = Some(parser)
    this
  }

  def withSaveResultProvider(saveParsedProvider: SaveParsedProvider): PipelineBuilder = {
    this.saveParsedProvider = Some(saveParsedProvider)
    this
  }

  def withProxy(proxyLimit: Int): PipelineBuilder = {
    resourceType = Some(Proxy(proxyLimit))
    this
  }

  def withTor(host: String, post: Int, limit: Int): PipelineBuilder = {
    resourceType = Some(Tor(host, post, limit))
    this
  }

  def build(): Pipeline = {
    if (taskType.isEmpty) {
      throw new PipelineBuilderException("task type is not defined")
    }

    if (batchSize.isEmpty) {
      throw new PipelineBuilderException("batch size is not defined")
    }

    if (crawlerCreator.isEmpty) {
      throw new PipelineBuilderException("crawler creator is not defined")
    }

    (parser, saveParsedProvider) match {
      case (Some(_), Some(_)) | (None, None) =>
      case _ => throw new PipelineBuilderException("parser or saveParsedProvider is not defined")
    }

    if (resourceType.isEmpty) {
      throw new PipelineBuilderException("proxy or tor is not defined")
    }

    Pipeline(
      taskType = taskType.get,
      batchSize = batchSize.get,
      crawlerCreator = crawlerCreator.get,
      saveRawProvider = saveRawProvider,
      parser = parser,
      saveParsedProvider = saveParsedProvider,
      resourceType = resourceType.get
    )
  }
}

object PipelineBuilder {
  def apply(): PipelineBuilder = new PipelineBuilder()
}

class PipelineBuilderException(message: String) extends Exception(message)
