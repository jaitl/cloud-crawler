package com.github.jaitl.cloud.base.parser

import com.github.jaitl.cloud.base.crawler.CrawlResult

trait BaseParser {
  def parse(crawlResult: CrawlResult): ParseResult
}
