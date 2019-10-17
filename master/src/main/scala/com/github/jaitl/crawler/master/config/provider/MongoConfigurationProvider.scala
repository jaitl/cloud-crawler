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
  private val crawlerProxies: MongoCollection[CrawlerProxy] =
    database.getCollection(proxyCollectionName)
  private val crawlerTors: MongoCollection[CrawlerTor] =
    database.getCollection(torCollectionName)

  override def getCrawlerProjectConfiguration(taskType: String): Future[Seq[ProjectConfiguration]] =
    crawlerProjectConfiguration
      .find(equal("workerTaskType", taskType))
      .toFuture()

  override def getCrawlerProxyConfiguration(taskType: String): Future[Seq[CrawlerProxy]] =
    crawlerProxies
      .find(equal("workerTaskType", taskType))
      .toFuture()

  override def getCrawlerTorConfiguration(taskType: String): Future[Seq[CrawlerTor]] =
    crawlerTors
      .find(equal("workerTaskType", taskType))
      .toFuture()
}
