package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.save.MongoTypeConverter
import org.json4s.jackson.JsonMethods.pretty
import org.json4s.jackson.JsonMethods.render
import org.json4s.Extraction
import org.json4s.FullTypeHints
import org.json4s.jackson.Serialization
import org.mongodb.scala.Document

class SODataMongoConverter extends MongoTypeConverter[StackowerflowParsedData] {
  override def convert(value: StackowerflowParsedData): Document = {
    implicit val formats = {
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowComments])))
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowHints])))
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowUser])))
    }
    val json = pretty(render(Extraction.decompose(value)))
    Document.apply(json)
  }
}
