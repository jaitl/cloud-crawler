package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.save.ElasticSearchTypeConverter
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization



class StackoverflowElasticsearchConverter extends ElasticSearchTypeConverter[StackowerflowParsedData] {
  override def convert(value: StackowerflowParsedData): String = {
    implicit val formats = {
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowComments])))
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowHints])))
      Serialization.formats(FullTypeHints(List(classOf[SatckoverflowUser])))
    }
    pretty(render(Extraction.decompose(value)))
  }
}
