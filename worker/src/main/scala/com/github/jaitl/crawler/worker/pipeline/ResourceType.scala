package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.timeout.RandomTimeout

private[worker] trait ResourceType

private[worker] case class Proxy(limit: Int, timeout: RandomTimeout) extends ResourceType

private[worker] case class Tor(host: String, port: Int, limit: Int, timeout: RandomTimeout) extends ResourceType
