package com.github.jaitl.cloud.base.parser

case class ParseResult(data: Map[String, String], newCrawlTasks: Map[String, Seq[String]] = Map.empty)
