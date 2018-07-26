package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexMaintenance extends AppLogging  {


  def createDelphiIndex(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")

    val client = HttpClient(configuration.elasticsearchClientUri)
    val featureList = ElasticFeatureListMapping.getMapAsSeq

    val identifierFields = Seq(
      //Git
      textField("repoUrl"),
      keywordField("commitId"),

      //Maven
      textField("groupId"),
      textField("artifactId"),
      keywordField("version")
    )

    val f = client.execute {
      createIndex(delphi) mappings (
        mapping(project) as (
          keywordField("name"),
          keywordField("source"),
          keywordField("language"),

          objectField("identifier") fields identifierFields,

          nestedField("calls") fields Seq(
            keywordField("name"),
            objectField("identifier") fields identifierFields,
            textField("methods") analyzer KeywordAnalyzer
          ),

          objectField("features") fields featureList
        )
      )

    }.await

    //Increases maximum number of nested fields
    client.execute{
      updateSettings(delphi).set(
        "index.mapping.nested_fields.limit", "250"
      )
    }.await

    Success(configuration)
  }

  def migrateIndex(configuration: Configuration)(implicit system: ActorSystem) : Try[Configuration] = {
    Success(configuration)
  }

  def isIndexCurrent(configuration: Configuration)(implicit system: ActorSystem) : Boolean = {
    true
  }
}
