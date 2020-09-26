package com.github.jaitl.crawler.master.queue.provider

import com.typesafe.config.Config

object QueueTaskProviderFactory {
  def getProvider(config: Config): QueueTaskProvider = {
    config.getString("provider") match {
      case "mongodb" =>
        val mongoConfig = config.getConfig("mongodb")
        new MongoQueueTaskProvider(
          connectionString = mongoConfig.getString("connectionString"),
          dbName = mongoConfig.getString("dbName"),
          collectionName = mongoConfig.getString("collectionName")
        )
      case "sql" =>
        val sqlConfig = config.getConfig("sql")
        new SqlQueueTaskProvider(
          connectionUrl = sqlConfig.getString("connectionUrl"),
          driverName = sqlConfig.getString("driverName"),
          user = sqlConfig.getString("user"),
          password = sqlConfig.getString("password")
        )
    }
  }
}
