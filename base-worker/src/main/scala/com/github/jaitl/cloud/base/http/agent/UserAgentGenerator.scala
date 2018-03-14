package com.github.jaitl.cloud.base.http.agent

import scala.util.Random

trait UserAgentGenerator {
  protected val windowsSeq: Seq[String] = Seq(
    "Windows NT 6.1; WOW64",
    "Windows NT 6.2; WOW64",
    "Windows NT 6.3; WOW64",
    "Windows NT 10; WOW64",
    "Windows NT 6.1; Win64; x64",
    "Windows NT 6.2; Win64; x64",
    "Windows NT 6.3; Win64; x64",
    "Windows NT 10; Win64; x64"
  )

  protected val linuxSeq: Seq[String] = Seq(
    "X11; Linux x86_64"
  )

  protected val macOsSeq: Seq[String] = Seq(
    "Macintosh; Intel Mac OS X 10_12_5",
    "Macintosh; Intel Mac OS X 10_12_6",
    "Macintosh; Intel Mac OS X 10_13_2",
    "Macintosh; Intel Mac OS X 10_13_3"
  )

  protected val desktopOsSeq: Seq[String] = windowsSeq ++ linuxSeq ++ macOsSeq

  def generate(): Seq[String]
}

object UserAgentGenerator {
  private val chrome = ChromeUserAgentGenerator.generate()
  private val firefox = FirefoxUserAgentGenerator.generate()
  private val edge = EdgeUserAgentGenerator.generate()
  private val opera = OperaUserAgentGenerator.generate()

  def randomUserAgent(): String = Random.shuffle(chrome ++ firefox ++ edge ++ opera).head
}
