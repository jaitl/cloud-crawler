package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.worker.parser.ParsedData

trait SqlTypeConverter[T <: ParsedData] {
  def convert(value: T): T = value
}
