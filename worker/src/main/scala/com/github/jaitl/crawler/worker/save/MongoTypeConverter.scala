package com.github.jaitl.crawler.worker.save

import org.mongodb.scala.Document

trait MongoTypeConverter[T] {
  def convert(value: T): Document
}
