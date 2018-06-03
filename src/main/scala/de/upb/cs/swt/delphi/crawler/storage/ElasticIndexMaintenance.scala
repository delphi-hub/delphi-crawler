package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldDefinition
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexMaintenance extends AppLogging  {


  def createDelphiIndex(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")

    val client = HttpClient(configuration.elasticsearchClientUri)
    val featureList: Seq[FieldDefinition] = ElasticFeatureListMapping.getMapAsSeq

    val f = client.execute {
      createIndex("delphi") mappings (
        mapping("project") as (
          keywordField("source"),
          keywordField("language"),

          objectField("identifier") fields Seq(
            //Git
            textField("repoUrl"),
            keywordField("commitId"),

            //Maven
            textField("groupId"),
            textField("artifactId"),
            keywordField("version")
          ),

          objectField("features") fields featureList
        )
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
