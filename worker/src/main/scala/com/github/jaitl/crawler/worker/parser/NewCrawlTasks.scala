package com.github.jaitl.crawler.worker.parser

case class NewCrawlTasks(projectId: String, taskType: String, tasks: Seq[String])
