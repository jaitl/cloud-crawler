package com.github.jaitl.crawler.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask

trait BaseParser[T] {
  @throws(classOf[ParsingException])
  def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[T]
}
final case class ParsingException(message: String = "", data: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
