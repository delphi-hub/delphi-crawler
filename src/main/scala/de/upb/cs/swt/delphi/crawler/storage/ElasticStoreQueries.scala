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
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.update.UpdateResponse
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import de.upb.cs.swt.delphi.crawler.discovery.git.GitIdentifier
import de.upb.cs.swt.delphi.crawler.discovery.maven.{MavenIdentifier, MavenProcessingError}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact
import de.upb.cs.swt.delphi.crawler.processing.{HermesAnalyzer, HermesResults}
import org.joda.time.DateTime

/**
  * Queries to map artifacts to elasticsearch insert or update queries.
  *
  * @author Ben Hermann
  * @author Alexander MacKenzie
  */
trait ElasticStoreQueries {
  this: ArtifactIdentityQuery =>

  def store(h: HermesResults)(implicit client: ElasticClient, log: LoggingAdapter): Option[Response[UpdateResponse]] = {
    elasticId(h.identifier) match {
      case Some(id) =>
        log.info(s"Pushing Hermes results for ${h.identifier} under id $id.")
        Some(client.execute {
          update(id).in(delphiProjectType).doc("hermes" -> Map(
            "features" -> h.featureMap,
            "version" -> HermesAnalyzer.HermesVersion,
            "runOn" -> DateTime.now()))
        }.await)
      case None => log.warning(s"Tried to push hermes results for non-existing identifier: ${h.identifier}."); None
    }
  }

  def store(m: MavenArtifact)(implicit client: ElasticClient, log: LoggingAdapter): Option[Response[UpdateResponse]] = {
    elasticId(m.identifier) match {
      case Some(id) =>
        log.info(s"Pushing POM file contents for ${m.identifier} under id $id")

        m.metadata match {
          case Some(metadata) =>
            Some(client.execute {
              update(id).in(delphiProjectType).doc(fields = "pom" -> Map(
                "name" -> metadata.name,
                "description" -> metadata.description,
                "issueManagement" -> metadata.issueManagement
                  .map(management => Map("url" -> management.url, "system" -> management.system)).getOrElse("None"),
                "developers" -> metadata.developers.mkString(","),
                "licenses" -> metadata.licenses.map(l => Map("name" -> l.name, "url" -> l.url)),
                "dependencies" -> metadata.dependencies.map(d => Map(
                  "groupId" -> d.identifier.groupId,
                  "artifactId" -> d.identifier.artifactId,
                  "version" -> d.identifier.version,
                  "scope" -> d.scope.getOrElse("default")
                )),
                "parent" -> metadata.parent.map(p => Map(
                  "groupId" -> p.groupId,
                  "artifactId" -> p.artifactId,
                  "version" -> p.version
                )).getOrElse("None"),
                "packaging" -> metadata.packaging
              ), "published" -> m.publicationDate.getOrElse("Unknown"))
            }.await)
          case None =>
            log.warning(s"Tried to push POM file results to database, but no results are present for identifier: ${m.identifier}")
            None
        }


      case None =>
        log.warning(s"Tried to push POM file results for non-existing identifier: ${m.identifier}.")
        None
    }
  }

  def store(error: MavenProcessingError)(implicit client: ElasticClient, log: LoggingAdapter): Response[IndexResponse]= {
    log.info(s"Pushing new error to elastic regarding identifier ${error.identifier}")
    client.execute {
      indexInto(delphiProcessingErrorType).id(error.occurredAt.getMillis.toString).fields(
        "identifier" -> Map(
          "groupId" -> error.identifier.groupId,
          "artifactId" -> error.identifier.artifactId,
          "version" -> error.identifier.version),
        "occurred" -> error.occurredAt,
        "message" -> error.message,
        "type" -> error.errorType.toString
      )
    }.await
  }

  def store(g: GitIdentifier)(implicit client: ElasticClient, log: LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new git identifier to elastic: [{}]", g)
    client.execute {
      indexInto(delphiProjectType).fields("name" -> (g.repoUrl + "/" + g.commitId),
        "source" -> "Git",
        "identifier" -> Map(
          "repoUrl" -> g.repoUrl,
          "commitId" -> g.commitId))
    }.await
  }

  def store(m: MavenIdentifier)(implicit client: ElasticClient, log: LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new maven identifier to elastic: [{}]", m.toUniqueString)
    client.execute {
      indexInto(delphiProjectType).id(m.toUniqueString)
        .fields("name" -> m.toUniqueString,
          "source" -> "Maven",
          "identifier" -> Map(
            "groupId" -> m.groupId,
            "artifactId" -> m.artifactId,
            "version" -> m.version),
          "discovered" -> DateTime.now())
    }.await
  }
}
