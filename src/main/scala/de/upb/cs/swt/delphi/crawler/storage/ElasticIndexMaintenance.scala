package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.MappingDefinition
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexMaintenance extends AppLogging  {

  def createDelphiMappings : Iterable[MappingDefinition] = { Seq(
    mapping("mavenArtifact").fields(
      textField("repository"),
      textField("groupId"),
      textField("artifactId"),
      textField("version")))
  }

  def createDelphiIndex(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")

    val client = HttpClient(configuration.elasticsearchClientUri)

    /*val f = client.execute {
      createIndex("delphi").mappings(createDelphiMappings)
    }.await*/

    Success(configuration)
  }

  def migrateIndex(configuration: Configuration)(implicit system: ActorSystem) : Try[Configuration] = {
    Success(configuration)
  }

  def isIndexCurrent(configuration: Configuration)(implicit system: ActorSystem) : Boolean = {
    true
  }
}
