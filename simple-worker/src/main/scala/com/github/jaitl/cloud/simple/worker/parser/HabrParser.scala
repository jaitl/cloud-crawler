package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.base.worker.crawler.CrawlResult
import com.github.jaitl.crawler.base.worker.crawler.CrawlTask
import com.github.jaitl.crawler.base.worker.parser.BaseParser
import com.github.jaitl.crawler.base.worker.parser.ParseResult

class HabrParser extends BaseParser[HabrParsedData] {
  override def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[HabrParsedData] = ???
}
