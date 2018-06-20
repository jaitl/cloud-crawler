package com.github.jaitl.crawler.worker.save

import org.mongodb.scala.Document
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MongoSaveParsedProvider[T](
  connectionString: String,
  dbName: String,
  collectionName: String
)(implicit converter: MongoTypeConverter[T]) extends SaveParsedProvider[T] {

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(collectionName)
  private val collection: MongoCollection[Document] = database.getCollection(collectionName)

  override def saveResults[D](parsedData: Seq[D])(implicit executionContext: ExecutionContext): Future[Unit] = {
    val docs = parsedData.map(d => converter.convert(d.asInstanceOf[T]))
    collection.insertMany(docs).toFuture().map(_ => Unit)
  }
}
