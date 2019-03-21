package com.github.jaitl.cloud.simple.worker.parser

import java.text.SimpleDateFormat

import com.github.jaitl.crawler.worker.crawler.CrawlResult
import com.github.jaitl.crawler.worker.crawler.CrawlTask
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.parser.ParseResult
import org.jsoup.Jsoup

import scala.collection.JavaConverters._

//scalastyle:off
class QaRuParser extends BaseParser[StackowerflowParsedData] {
  val dateFormat = new SimpleDateFormat("yyyy-mm-dd HH:mm")

  override def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[StackowerflowParsedData] = {
    val doc = Jsoup.parse(crawlResult.data)

    val date = 0

    val title = doc.select("title").text()

    val content = doc.select("div.question-text div.description").html()
    val url = doc.select("link[rel=\"canonical\"]").attr("href")
    val sourceUrl = doc.select("div.question a.aa-link").attr("href")
    val id = doc.select("link[rel=\"canonical\"]").attr("href").split("/")(4).toLong

    val tags = doc.select("div.question-text div.tags a").asScala.map { el =>
      el.text().trim
    }

    val qVote = doc.select("div.question-text div.vote-count").text().toInt

    val comments = doc.select("div.answers div.answer").asScala.map { el =>
      SatckoverflowComments(
        el.select("div.desc").html(),
        el.attr("id").toLong,
        0,
        SatckoverflowUser(
          el.select("div.action-time a")
            .attr("href")
            .split("/")
            .lift(4)
            .map { _.toLong }
            .getOrElse(0),
          el.select("div.action-time a").text(),
          el.select("div.action-time a").attr("href")
        ),
        Seq(),
        el.select("div.vote-count").text().trim.toInt,
        el.attr("class").contains("accepted")
      )
    }
    val user = doc
      .select("div.action-time")
      .asScala
      .map { el =>
        if (el.select("a").size() == 0) {
          SatckoverflowUser(
            -1,
            "Anonymous",
            "https://codeindex.ru/user/-1"
          )
        } else {
          SatckoverflowUser(
            el.select("a")
              .attr("href")
              .split("/")
              .lift(4)
              .map { _.toLong }
              .getOrElse(0),
            el.select("a").text(),
            el.select("a").attr("href")
          )
        }
      }
      .head

    ParseResult(StackowerflowParsedData(title, content, url, id, date, tags, comments, Seq(), user, qVote, sourceUrl))
  }
}
