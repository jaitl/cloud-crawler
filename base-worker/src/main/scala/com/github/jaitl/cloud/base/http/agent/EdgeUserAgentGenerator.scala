package com.github.jaitl.cloud.base.http.agent

object EdgeUserAgentGenerator extends UserAgentGenerator {
  private val format = "Mozilla/5.0 (%s) AppleWebKit/%s (KHTML, like Gecko) Chrome/%s Safari/%s Edge/%s"

  // scalastyle:off multiple.string.literals
  private val edgeSeq: Seq[(String, String, String)] = Seq(
    ("537.36", "46.0.2486.0", "13.10586"),
    ("537.36", "51.0.2704.79", "14.14393"),
    ("537.36", "52.0.2743.116", "15.15063"),
    ("537.36", "64.0.3282.140", "16.16299")
  )
  // scalastyle:on


  override def generate(): Seq[String] = {
    for {
      os <- windowsSeq
      (webKit, chrome, edge) <- edgeSeq
    } yield String.format(format, os, webKit, chrome, webKit, edge)
  }
}
