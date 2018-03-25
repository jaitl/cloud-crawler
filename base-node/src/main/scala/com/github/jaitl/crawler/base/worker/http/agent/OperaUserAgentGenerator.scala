package com.github.jaitl.crawler.base.worker.http.agent

object OperaUserAgentGenerator extends UserAgentGenerator {
  private val format = "Mozilla/5.0 (%s) AppleWebKit/%s (KHTML, like Gecko) Chrome/%s Safari/%s OPR/%s"

  // scalastyle:off multiple.string.literals
  private val versionSeq: Seq[(String, String, String)] = Seq(
    ("537.36", "49.0.2623.87", "36.0.2130.46"),
    ("537.36", "53.0.2785.101", "40.0.2308.62"),
    ("537.36", "56.0.2924.87", "43.0.2442.1144"),
    ("537.36", "57.0.2987.98", "44.0.2510.857"),
    ("537.36", "59.0.3071.115", "46.0.2597.39"),
    ("537.36", "61.0.3163.100", "48.0.2685.35"),
    ("537.36", "62.0.3202.94", "49.0.2725.64"),
    ("537.36", "63.0.3239.132", "50.0.2762.58")
  )
  // scalastyle:on

  override def generate(): Seq[String] = {
    for {
      os <- desktopOsSeq
      (webKit, chrome, opr) <- versionSeq
    } yield String.format(format, os, webKit, chrome, webKit, opr)
  }
}
