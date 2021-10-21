// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.storage

import akka.event.LoggingAdapter
import com.sksamuel.elastic4s.{ElasticClient, Response}
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.model.{MavenArtifactMetadata, ProcessingError}
import de.upb.cs.swt.delphi.crawler.processing.{HermesAnalyzer, HermesResults}
import org.joda.time.DateTime

/**
  * Queries to map artifacts to elasticsearch insert or update queries.
  *
  * @author Ben Hermann
  * @author Alexander MacKenzie
  * @author Johannes Düsing
  */
trait ElasticStoreQueries {

  this: ArtifactIdentityQuery =>

  import com.sksamuel.elastic4s.ElasticDsl._


  def store(h: HermesResults)(implicit client: ElasticClient, log: LoggingAdapter): Option[Response[IndexResponse]] = {
    elasticId(h.identifier) match {
      case Some(id) =>
        log.info(s"Pushing Hermes results for ${h.identifier} under id $id.")
        Some(client.execute {
          indexInto(metricIndexName).id(id).fields(
            "features" -> h.featureMap,
            "version" -> HermesAnalyzer.HermesVersion,
            "runOn" -> DateTime.now())
        }.await)
      case None => log.warning(s"Tried to push hermes results for non-existing identifier: ${h.identifier}."); None
    }
  }

  def store(m: MavenIdentifier)(implicit client: ElasticClient, log: LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new maven identifier to elastic: [{}]", m)
    client.execute {
      indexInto(identifierIndexName).id(m.toUniqueString)
        .fields("name" -> m.toUniqueString,
          "source" -> "Maven",
          "identifier" -> Map(
            "groupId" -> m.groupId,
            "artifactId" -> m.artifactId,
            "version" -> m.version.get),
          "discovered" -> DateTime.now())
    }.await
  }

  def store(error: ProcessingError)(implicit client: ElasticClient, log: LoggingAdapter): Option[Response[IndexResponse]] = {
    elasticId(error.identifier) match {
      case Some(id) =>
        val result = client.execute{
            indexInto(errorIndexName).fields(
              "name" -> id,
              "phase" -> error.phase.toString,
              "message" -> error.message,
              "identifier" -> Map(
                "groupId" -> error.identifier.groupId,
                "artifactId" -> error.identifier.artifactId,
                "version" -> error.identifier.version.get
              ),
            )
          }.await
        Some(result)
      case None =>
        log.warning(s"Tried to push error for non-existing identifier: ${error.identifier.toString}.")
        None
    }
  }

  def store(ident: MavenIdentifier, metadata: MavenArtifactMetadata, published: Option[DateTime])
           (implicit client: ElasticClient, log: LoggingAdapter): Option[Response[IndexResponse]] = {
    elasticId(ident) match {
      case Some(id) =>
        log.info(s"Pushing metadata results for $ident under id $id.")
        val result = client.execute {
          indexInto(metadataIndexName).id(id).fields(
            "name" -> metadata.name,
            "identifier" -> Map(
              "groupId" -> ident.groupId,
              "artifactId" -> ident.artifactId,
              "version" -> ident.version.get
            ),
            "packaging" -> metadata.packaging,
            "description" -> metadata.description,
            "parent" -> metadata
              .parent
              .map(m => Map("groupId" -> m.groupId, "artifactId" -> m.artifactId, "version" -> m.version.get))
              .getOrElse(Map.empty),
            "licenses" -> metadata.licenses.map(_.toString).mkString(","),
            "issueManagement" -> metadata.issueManagement.map(_.toString).getOrElse("None"),
            "developers" -> metadata.developers.mkString(","),
            "dependencies" -> metadata.dependencies.map{ dep =>
              Map("groupId" -> dep.identifier.groupId, "artifactId" -> dep.identifier.artifactId,
                "version" -> dep.identifier.version.get, "scope" -> dep.scope.getOrElse("default"))
            },
            "published" -> published.getOrElse("Unknown")
          )
        }.await
        Some(result)
      case None =>
        log.warning(s"Tried to push metadata results for non-existing identifier: $ident.")
        None
    }
  }
}
