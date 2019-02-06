package com.github.jaitl.crawler.worker.save

trait ElasticSearchTypeConverter[T] {
  def convert(value: T): String
}

