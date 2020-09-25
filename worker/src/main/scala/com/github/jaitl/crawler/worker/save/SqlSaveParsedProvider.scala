package com.github.jaitl.crawler.worker.save

import com.github.jaitl.crawler.worker.parser.ParsedData

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalikejdbc.ConnectionPool
import scalikejdbc._

class SqlSaveParsedProvider[T <: ParsedData](
  connectionUrl: String,
  driverName: String,
  user: String,
  password: String
)(implicit converter: SqlTypeConverter[T])
    extends SaveParsedProvider[T] {
  Class.forName(driverName)
  ConnectionPool.singleton(connectionUrl, user, password)
  override def saveResults(parsedData: Seq[T])(implicit executionContext: ExecutionContext): Future[Unit] = {
    val docs = parsedData.map(d => converter.convert(d))
    Future.successful(docs.foreach(source =>
      DB.localTx { implicit session =>
        sql"update projects_url set source = ${source.source} where id = ${source.id}".update().apply()
    }))
  }
}
