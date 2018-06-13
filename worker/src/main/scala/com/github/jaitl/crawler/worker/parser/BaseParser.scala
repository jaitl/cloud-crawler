package com.github.jaitl.crawler.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask

trait BaseParser[T] {
  def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[T]
}
