package com.github.jaitl.cloud.simple.worker.parser

import java.text.SimpleDateFormat

import com.github.jaitl.crawler.worker.crawler.{CrawlResult, CrawlTask}
import com.github.jaitl.crawler.worker.parser.{BaseParser, ParseResult}
import org.jsoup.Jsoup

import scala.collection.JavaConverters._

//scalastyle:off
class StackoverrunParser extends BaseParser[StackowerflowParsedData] {
  val dateFormat = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss")

  override def parse(crawlTask: CrawlTask, crawlResult: CrawlResult): ParseResult[StackowerflowParsedData] = {
    val doc = Jsoup.parse(crawlResult.data)

    val date = dateFormat.parse(
      doc.select("div.row div.row div.col-lg-11.col-md-12 p.text-secondary.createDate.col-lg-6.col-md-6.col-sm-6.col-xs-12")
        .get(0).select("small span.d-none")
      .attr("datetime")
    ).getTime

    val title = doc.select("h1").text()

    val content = doc.select("div.row div.row div.post-text").get(0).html()
    val url = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.col-lg-11.col-md-12 > div.post-info > div.post-meta.row > p:nth-child(1) > small:nth-child(1) > a").attr("href")
    val id = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.col-lg-11.col-md-12 div.post-text")
      .attr("id").replace("text_q", "").toLong

    val tags = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.col-lg-11.col-md-12 > div.post-info > div.post-taglist span.post-tag")
      .asScala.filter(el => !el.attr("class").contains("view-count")).map {
      el => el.text()
    }

    val qVote = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.voteInfo.col-1.d-none.d-lg-block > span.count").text().toInt

    val comments = doc.select("div.answersList div.answer-block").asScala.map {
      el =>
        val hints = el.select("div.comments").asScala.map {
          h => {
            SatckoverflowHints(
              h.select("p.commenttext span.post-text").html(),
              h.select("p.commenttext span.post-text").attr("id").replace("text_c", "").toLong,
              dateFormat.parse(h.select("span.text-secondary span.d-none")
              .attr("datetime")
            ).getTime,
              SatckoverflowUser(
                h.select("span.text-secondary a").attr("href").split("/")
                  .lift(4).map{_.toLong}.getOrElse(0),
                h.select("span.text-secondary a").text(),
                h.select("span.text-secondary a").attr("href")
              ),
//              h.select("span.text-secondary").text().replace("+","").toInt
            )
          }
        }
        SatckoverflowComments(
          el.select("div.post-text").html(),
          el.attr("id").toLong,
          dateFormat.parse(el.select("span.d-none")
            .attr("datetime")
          ).getTime,
          SatckoverflowUser(
            el.select("div.post-info p.text-secondary small span a").attr("href").split("/")
              .lift(4).map{_.toLong}.getOrElse(0),
            el.select("div.post-info p.text-secondary small span a").text(),
            el.select("div.post-info p.text-secondary small span a").attr("href")

          ),
          hints,
          el.select("span.count").text().trim.toInt,
          false
        )
    }
    val user = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.col-lg-11.col-md-12 > div.post-info > div.post-meta.row > p.text-secondary.createDate.col-lg-6.col-md-6.col-sm-6.col-xs-12 > small")
      .asScala.map {
      el =>
        if(el.select("a").size() == 0) {
            SatckoverflowUser(
              -1,
              "Anonymous",
              "https://codeindex.ru/user/-1"
            )
        } else {
          SatckoverflowUser(
            el.select("a").attr("href").split("/")
              .lift(4).map{_.toLong}.getOrElse(0),
            el.select("a").text(),
            el.select("a").attr("href")

          )
        }
    }.head


    val hints = doc.select("body > main > div > div.col-lg-9.col-md-12.col-sm-12 > div > div.col-lg-11.col-md-12 > div.comments").asScala.map {
      el =>
        SatckoverflowHints(
          el.select("div.post-comment  span.post-text").html(),
          el.select("div.post-comment  span.post-text").attr("id").replace("text_c","").toLong,
          dateFormat.parse(el.select("span.text-secondary span.d-none")
            .attr("datetime")
          ).getTime,
          SatckoverflowUser(
            el.select("div.comments div.post-comment div.col-11  span.text-secondary a").attr("href").split("/")
              .lift(4).map{_.toLong}.getOrElse(0),
            el.select("div.comments div.post-comment div.col-11  span.text-secondary a").text(),
            el.select("div.comments div.post-comment div.col-11  span.text-secondary a").attr("href")
          ),
          //el.select("span.text-secondary").text().replace("+","").toInt
        )
    }

    ParseResult(StackowerflowParsedData(title, content, url, id, date, tags, comments, hints, user, qVote))
  }
}
