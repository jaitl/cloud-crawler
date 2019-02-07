package com.github.jaitl.cloud.simple.worker.parser

case class StackowerflowParsedData(
                               title: String,
                               body: String,
                               url: String,
                               id: Long,
                               date: Long,
                               tags: Seq[String],
                               comments: Seq[SatckoverflowComments],
                               hints: Seq[SatckoverflowHints],
                               user: SatckoverflowUser
                             )

case class SatckoverflowComments(
                                  body: String,
                                  id: Long,
                                  date: Long,
                                  user: SatckoverflowUser,
                                  hints: Seq[SatckoverflowHints]
                                )
case class SatckoverflowHints(
                                  body: String,
                                  id: Long,
                                  date: Long,
                                  user: SatckoverflowUser
                                )

case class SatckoverflowUser(id: Long, name: String, url: String)
