package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.io.Source

class QaRuParserTest extends FunSuite with Matchers {
  test("QaRu") {
    val content = Source.fromResource("html/qaru.html").mkString
    val parser = new QaRuParser

    val res = parser
      .parse(CrawlTask("1000", "QaRuTasks"), CrawlResult(content))
      .parsedData

    res.tags.size shouldBe 4
    res.tags shouldEqual Seq("artificial-intelligence", "matlab", "classification", "perceptron")
    res.url shouldBe "http://qaru.site/questions/3190434/single-layer-neural-network"
    res.sourceUrl shouldBe "https://stackoverflow.com/questions/3455660/single-layer-neural-network"
    //res.date shouldBe 1264778427000L
    res.title shouldBe "artificial-intelligence - Однослойная нейронная сеть - Qaru"

    res.user.name shouldBe "user414981"
    res.user.id shouldBe 414981L
    res.user.url shouldBe "https://stackoverflow.com/users/414981/user414981"

    res.voteCount shouldBe -73

    res.comments.size shouldBe 1
    res.comments.head.id shouldBe 7868921L
    res.comments.head.user shouldBe SatckoverflowUser(97160L, "Amro", "https://stackoverflow.com/users/97160/amro")
    //res.comments.head.date shouldBe 1264778855000L
    res.comments.head.accepted shouldBe false
    res.comments.head.voteCount shouldBe 222
  }
  test("QaRu 2") {
    val content = Source.fromResource("html/qaru_2.html").mkString
    val parser = new QaRuParser

    val res = parser
      .parse(CrawlTask("1000", "QaRuTasks"), CrawlResult(content))
      .parsedData

    res.tags.size shouldBe 3
    res.tags shouldEqual Seq("git", "git-branch", "git-remote")
    res.url shouldBe "http://qaru.site/questions/3/how-to-delete-a-git-branch-both-locally-and-remotely"
    res.sourceUrl shouldBe "https://stackoverflow.com/questions/2003505/how-do-i-delete-a-git-branch-both-locally-and-remotely"
    //res.date shouldBe 1264778427000L
    res.title shouldBe "git - Как удалить ветвь Git как локально, так и удаленно? - Qaru"
    res.id shouldBe 3

    res.user.name shouldBe "Matthew Rankin"
    res.user.id shouldBe 95592L
    res.user.url shouldBe "https://stackoverflow.com/users/95592/matthew-rankin"

    res.voteCount shouldBe 14380

    res.comments.size shouldBe 30
    res.comments.head.id shouldBe 75L
    res.comments.head.user shouldBe SatckoverflowUser(95592L, "Matthew Rankin", "https://stackoverflow.com/users/95592/matthew-rankin")
    //res.comments.head.date shouldBe 1264778855000L
    res.comments.head.accepted shouldBe true
    res.comments.head.voteCount shouldBe 17997
  }
  test("QaRu 3") {
    val content = Source.fromResource("html/qaru_3.html").mkString
    val parser = new QaRuParser

    val res = parser
      .parse(CrawlTask("1000", "QaRuTasks"), CrawlResult(content))
      .parsedData

    res.tags.size shouldBe 3
    res.url shouldBe "http://qaru.site/questions/5802709/recorded-files-lost-when-user-hangs-up-in-asterisk"
  }

}
