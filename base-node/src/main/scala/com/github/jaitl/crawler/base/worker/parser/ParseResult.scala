package com.github.jaitl.crawler.base.worker.parser

case class ParseResult(data: Map[String, String], newCrawlTasks: Map[String, Seq[String]] = Map.empty)
