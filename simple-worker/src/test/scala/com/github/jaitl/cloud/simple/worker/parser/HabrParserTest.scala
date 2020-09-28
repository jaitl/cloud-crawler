package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.io.Source

class HabrParserTest extends FunSuite with Matchers {
  test("habr.com/post/1000") {
    val content = Source.fromResource("html/1k_post.html").mkString
    val parser = new HabrParser

    val res = parser.parse(CrawlTask("1", "1000", "HabrTasks", "1", null, "habr.com"), CrawlResult("1", content)).parsedData

    res.author shouldBe "Maxime"
    res.title shouldBe "Wikiasaria — комедия ошибок"
    res.content.startsWith("Волна публикаций, второй день бродящая в инете") shouldBe true
  }
}
