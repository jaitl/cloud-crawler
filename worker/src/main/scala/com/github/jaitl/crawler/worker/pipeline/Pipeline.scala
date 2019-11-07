package com.github.jaitl.crawler.worker.pipeline

import com.github.jaitl.crawler.worker.crawler.BaseCrawler
import com.github.jaitl.crawler.worker.email.BaseNotification
import com.github.jaitl.crawler.worker.parser.BaseParser
import com.github.jaitl.crawler.worker.save.SaveParsedProvider
import com.github.jaitl.crawler.worker.save.SaveRawProvider

private[worker] case class Pipeline[T](
                                        taskType: String,
                                        crawler: BaseCrawler,
                                        emailNotifier: Option[BaseNotification],
                                        saveRawProvider: Option[SaveRawProvider],
                                        parser: Option[BaseParser[T]],
                                        saveParsedProvider: Option[SaveParsedProvider[T]]
)
