package com.github.jaitl.crawler.worker.parser

case class ParseResult[T](parsedData: T, newCrawlTasks: Seq[NewCrawlTasks] = Seq.empty)
