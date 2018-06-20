package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.save.MongoTypeConverter
import org.mongodb.scala.Document

class HabrDataMongoConverter extends MongoTypeConverter[HabrParsedData] {
  override def convert(value: HabrParsedData): Document = {
    Document("author" -> value.author, "title" -> value.title, "content" -> value.content)
  }
}
