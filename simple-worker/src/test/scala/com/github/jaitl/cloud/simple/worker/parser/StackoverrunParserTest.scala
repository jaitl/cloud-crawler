package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.io.Source

class StackoverrunParserTest extends FunSuite with Matchers {
  test("Stackoverrun") {
    val content = Source.fromResource("html/sr.html").mkString
    val parser = new StackoverrunParser

    val res = parser
      .parse(CrawlTask("1000", "StackoverrunTasks"), CrawlResult(content))
      .parsedData

    res.tags.size shouldBe 2
    res.tags shouldEqual Seq("android", "alarmmanager")
    res.url shouldBe "https://stackoverrun.com/ru/q/1085109"
    res.sourceUrl shouldBe "https://stackoverflow.com/q/4556670"
    res.date shouldBe 1264778427000L
    res.title shouldBe "Как проверить, установлен ли AlarmManager уже установленный будильник?"

    res.user.name shouldBe "ron"
    res.user.id shouldBe 538847
    res.user.url shouldBe "https://stackoverflow.com/users/538847/"

    res.hints.size shouldBe 1
    res.hints.head.user shouldEqual SatckoverflowUser(
      3503855L,
      "AnixPasBesoin",
      "https://stackoverflow.com/users/3503855/")
    res.hints.head.id shouldBe 70568869L
    //res.hints.head.date shouldBe 1546428410000L
    res.hints.head.body shouldBe "Пожалуйста, подтвердите ответ, что решить проблему или разместить собственное решение."
    res.hints.head.voteCount shouldBe 0
    res.voteCount shouldBe 182

    res.comments.size shouldBe 10
    res.comments.head.id shouldBe 4556735L
    res.comments.head.user shouldBe SatckoverflowUser(
      526836L,
      "fiction",
      "https://stackoverflow.com/users/526836/")
    res.comments.head.date shouldBe 1264778855000L
    res.comments.head.accepted shouldBe false
  }

  test("Stackoverrun-npe") {
    val content = Source.fromResource("html/sr_npe.html").mkString
    val parser = new StackoverrunParser

    val res = parser
      .parse(CrawlTask("1000", "StackoverrunTasks"), CrawlResult(content))
      .parsedData
    res.title shouldBe "Ошибка в рисовании графики"

    res.user.name shouldBe "Anonymous"
    res.user.id shouldBe -1
    res.user.url shouldBe "https://codeindex.ru/user/-1"
  }
  test("Stackoverrun-parse") {
    val content = Source.fromResource("html/sr_parse.html").mkString
    val parser = new StackoverrunParser

    val res = parser
      .parse(CrawlTask("1000", "StackoverrunTasks"), CrawlResult(content))
      .parsedData
    res.title shouldBe "Удалить случайную строку между двумя строками android"

    res.user.name shouldBe "user934820"
    res.user.id shouldBe 934820
  }

}
