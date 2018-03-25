package com.github.jaitl.crawler.base.worker.parser

import com.github.jaitl.crawler.base.worker.crawler.CrawlResult

trait BaseParser {
  def parse(crawlResult: CrawlResult): ParseResult
}
