package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.save.SqlTypeConverter

class HabrDataSQLConverter extends SqlTypeConverter[HabrParsedData] {
  override def convert(value: HabrParsedData): HabrParsedData = value
}
