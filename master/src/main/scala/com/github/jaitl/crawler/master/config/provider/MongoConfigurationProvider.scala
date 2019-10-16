package com.github.jaitl.crawler.master.config.provider

import com.github.jaitl.crawler.models.worker.ProjectConfiguration
import com.github.jaitl.crawler.models.worker.CrawlerProxy
import com.github.jaitl.crawler.models.worker.CrawlerTor
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MongoConfigurationProvider(
  connectionString: String,
  dbName: String,
  configurationCollectionName: String,
  proxyCollectionName: String,
  torCollectionName: String,
) extends CrawlerConfigurationProvider {

  import org.mongodb.scala.bson.codecs.Macros._ // scalastyle:ignore
  import org.mongodb.scala.model.Filters._ // scalastyle:ignore
  import org.mongodb.scala.model.Updates._ // scalastyle:ignore

  private val mongoClient: MongoClient = MongoClient(connectionString)
  private val database: MongoDatabase = mongoClient.getDatabase(dbName)
  private val crawlerProjectConfiguration: MongoCollection[ProjectConfiguration] =
    database.getCollection(configurationCollectionName)
  private val crawlerProxies: MongoCollection[ProjectConfiguration] =
    database.getCollection(proxyCollectionName)
  private val crawlerTors: MongoCollection[ProjectConfiguration] =
    database.getCollection(torCollectionName)

  override def getCrawlerProjectConfiguration(taskType: String)(
    implicit ec: ExecutionContext): Future[Option[ProjectConfiguration]] =
    crawlerProjectConfiguration
      .find(equal("workerTaskType", taskType))
      .toFuture()
      .map(_.headOption)

  override def getCrawlerProxyConfiguration(taskType: String): Future[Option[CrawlerProxy]] = ???

  override def getCrawlerTOrConfiguration(taskType: String): Future[Option[CrawlerTor]] = ???
}
