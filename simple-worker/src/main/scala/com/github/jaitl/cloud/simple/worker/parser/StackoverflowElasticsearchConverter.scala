package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.save.ElasticSearchTypeConverter
import com.google.gson.Gson


class StackoverflowElasticsearchConverter extends ElasticSearchTypeConverter[StackowerflowParsedData] {
  override def convert(value: StackowerflowParsedData): String = {
    new Gson().toJson(value)
  }
}
