package com.github.jaitl.cloud.simple.worker.parser

import com.github.jaitl.crawler.worker.parser.ParsedData

case class HabrParsedData(id: String, source: String, author: String, title: String, content: String) extends ParsedData
