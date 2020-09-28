package com.github.jaitl.crawler.worker.parser

import com.github.jaitl.crawler.master.client.task.Task

case class ParseResult[T](parsedData: T, newCrawlTasks: Seq[Task] = Seq.empty)
