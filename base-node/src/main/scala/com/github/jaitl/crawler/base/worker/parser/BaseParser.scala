package com.github.jaitl.crawler.base.worker.parser

import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.crawler.CrawlTask

trait BaseParser[T <: ParsedData] {
  def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[T]
}
