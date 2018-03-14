package com.github.jaitl.cloud.base.http.agent

object FirefoxUserAgentGenerator extends UserAgentGenerator {
  private val format = "Mozilla/5.0 (%s; rv:%s) Gecko/%s Firefox/%s"

  // scalastyle:off multiple.string.literals
  private val firefoxVersionSeq: Seq[(String, String)] = Seq(
    ("20.0", "20100101"),
    ("30.0", "20100101"),
    ("40.1", "20100101"),
    ("45.59.06", "20157849"),
    ("46.0", "20100101"),
    ("49.0", "20100101")
  )
  // scalastyle:on

  override def generate(): Seq[String] = {
    for {
      os <- desktopOsSeq
      (gecko, firefox) <- firefoxVersionSeq
    } yield String.format(format, os, firefox, gecko, firefox)
  }
}
