package com.github.jaitl.crawler.base.worker.parser

case class ParseResult[T <: ParsedData](parsedData: T, newCrawlTasks: Seq[NewCrawlTasks] = Seq.empty)
