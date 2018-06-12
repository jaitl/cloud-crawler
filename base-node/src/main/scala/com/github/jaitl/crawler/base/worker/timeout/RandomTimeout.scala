package com.github.jaitl.crawler.base.worker.timeout

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

case class RandomTimeout(center: Duration, radius: Duration) {
  def computeRandom: Duration = {
    val minTimeout = (center - radius).toMillis
    val maxTimeout = (center + radius).toMillis
    val randomTimeout = ((maxTimeout - minTimeout) + 1) * math.random() + minTimeout

    Duration(randomTimeout.toLong, TimeUnit.MILLISECONDS)
  }
}
