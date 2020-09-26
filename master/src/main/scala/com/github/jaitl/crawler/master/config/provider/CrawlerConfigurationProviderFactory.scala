package com.github.jaitl.crawler.master.config.provider

import com.typesafe.config.Config

object CrawlerConfigurationProviderFactory {
  def getProvider(config: Config): CrawlerConfigurationProvider =
    config.getString("provider") match {
      case "mongodb" =>
        val mongoConfig = config.getConfig("mongodb")
        new MongoConfigurationProvider(
          connectionString = mongoConfig.getString("connectionString"),
          dbName = mongoConfig.getString("dbName"),
          configurationCollectionName = mongoConfig.getString("configurationCollectionName"),
          proxyCollectionName = mongoConfig.getString("proxyCollectionName"),
          torCollectionName = mongoConfig.getString("torCollectionName")
        )
      case "sql" =>
        val sqlConfig = config.getConfig("sql")
        new SqlConfigurationProvider(
          connectionUrl = sqlConfig.getString("connectionUrl"),
          driverName = sqlConfig.getString("driverName"),
          user = sqlConfig.getString("user"),
          password = sqlConfig.getString("password")
        )
    }
}
