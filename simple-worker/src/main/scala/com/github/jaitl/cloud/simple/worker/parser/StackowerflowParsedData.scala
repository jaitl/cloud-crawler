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
  user: SatckoverflowUser,
  voteCount: Int,
  sourceUrl: String = ""
)

case class SatckoverflowComments(
  body: String,
  id: Long,
  date: Long,
  user: SatckoverflowUser,
  hints: Seq[SatckoverflowHints],
  voteCount: Int,
  accepted: Boolean
)
case class SatckoverflowHints(
  body: String,
  id: Long,
  date: Long,
  user: SatckoverflowUser,
  voteCount: Int = 0
)

case class SatckoverflowUser(id: Long, name: String, url: String)
