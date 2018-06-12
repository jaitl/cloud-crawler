package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.crawler.BaseCrawlerCreator
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.NoParser
import com.github.jaitl.crawler.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.worker.save.SaveRawProvider
import com.github.jaitl.crawler.worker.timeout.RandomTimeout

private[pipeline] class PipelineBuilder[T] {
  private var taskType: Option[String] = None
  private var batchSize: Option[Int] = None
  private var crawlerCreator: Option[BaseCrawlerCreator] = None
  private var saveRawProvider: Option[SaveRawProvider] = None
  private var parser: Option[BaseParser[T]] = None
  private var saveParsedProvider: Option[SaveParsedProvider[T]] = None
  private var resourceType: Option[ResourceType] = None

  def withTaskType(taskType: String): this.type = {
    this.taskType = Some(taskType)
    this
  }

  def withBatchSize(batchSize: Int): this.type = {
    this.batchSize = Some(batchSize)
    this
  }

  def withCrawlerCreator(crawlerCreator: BaseCrawlerCreator): this.type = {
    this.crawlerCreator = Some(crawlerCreator)
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

  def withProxy(proxyLimit: Int, timeout: RandomTimeout): this.type = {
    resourceType = Some(Proxy(proxyLimit, timeout))
    this
  }

  def withTor(host: String, post: Int, limit: Int, timeout: RandomTimeout): this.type = {
    resourceType = Some(Tor(host, post, limit, timeout))
    this
  }

  def build(): Pipeline[T] = {
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
  def apply[T](): PipelineBuilder[T] = new PipelineBuilder[T]()
  def noParserPipeline(): PipelineBuilder[NoParser] = new PipelineBuilder[NoParser]()
}

class PipelineBuilderException(message: String) extends Exception(message)
