package com.github.jaitl.cloud.base.http.agent

object ChromeUserAgentGenerator extends UserAgentGenerator {
  private val format = "Mozilla/5.0 (%s) AppleWebKit/%s (KHTML, like Gecko) Chrome/%s Safari/%s"

  // scalastyle:off multiple.string.literals
  private val chromeWebKitSeq: Seq[(String, String)] = Seq(
    ("56.0.2924", "537.36"),
    ("57.0.2987", "537.36"),
    ("58.0.3029", "537.36"),
    ("59.0.3071", "537.36"),
    ("60.0.3112", "537.36"),
    ("61.0.3163", "537.36"),
    ("62.0.3202", "537.36"),
    ("63.0.3239", "537.36"),
    ("64.0.3282", "537.36"),
    ("65.0.3325", "537.36"),
    ("65.0.3325", "537.36"),
    ("66.0", "537.36"),
    ("67.0", "537.36")
  )
  // scalastyle:on

  override def generate(): Seq[String] = {
    for {
      os <- desktopOsSeq
      (chrome, webKit) <- chromeWebKitSeq
    } yield String.format(format, os, webKit, chrome, webKit)
  }
}
