package com.github.jaitl.crawler.base.worker.pipeline

private[base] trait ResourceType

private[base] case class Proxy(limit: Int) extends ResourceType

private[base] case class Tor(host: String, port: Int, limit: Int) extends ResourceType
