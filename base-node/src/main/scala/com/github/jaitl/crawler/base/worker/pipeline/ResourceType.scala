package com.github.jaitl.crawler.base.worker.pipeline

import com.github.jaitl.crawler.base.worker.timeout.RandomTimeout

private[base] trait ResourceType

private[base] case class Proxy(limit: Int, timeout: RandomTimeout) extends ResourceType

private[base] case class Tor(host: String, port: Int, limit: Int, timeout: RandomTimeout) extends ResourceType
