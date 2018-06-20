package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.ParseResult
import org.jsoup.Jsoup

class HabrParser extends BaseParser[HabrParsedData] {
  override def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[HabrParsedData] = {
    val doc = Jsoup.parse(crawlResult.data)

    val author = doc.select("header.post__meta span.user-info__nickname").text()
    val title = doc.select("h1.post__title").text()
    val content = doc.select("div.post__body").text()

    ParseResult(HabrParsedData(author, title, content))
  }
}
