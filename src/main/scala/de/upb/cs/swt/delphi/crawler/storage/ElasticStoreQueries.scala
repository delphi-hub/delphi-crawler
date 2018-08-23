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
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.HermesResults

/**
  * Queries to map artifacts to elasticsearch insert or update queries.
  *
  * @author Ben Hermann
  * @author Alexander MacKenzie
  */
trait ElasticStoreQueries {
  this: ArtifactIdentityQuery =>

  def store(h: HermesResults)(implicit client: ElasticClient, log : LoggingAdapter): Option[Response[UpdateResponse]] = {
    elasticId(h.identifier) match {
      case Some(id) =>
        log.info(s"Pushing Hermes results for ${h.identifier} under id $id.")
        Some(client.execute {
          update(id).in(delphiProjectType).doc("features" -> h.featureMap)
        }.await)
      case None => log.warning(s"Tried to push hermes results for non-existing identifier: ${h.identifier}."); None
    }
  }

  def store(g: GitIdentifier)(implicit client: ElasticClient,log : LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new git identifier to elastic: [{}]", g)
    client.execute {
      indexInto(delphiProjectType).fields("name" -> (g.repoUrl + "/" + g.commitId),
        "source" -> "Git",
        "identifier" -> Map(
          "repoUrl" -> g.repoUrl,
          "commitId" -> g.commitId))
    }.await
  }

  def store(m: MavenIdentifier)(implicit client: ElasticClient, log : LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new maven identifier to elastic: [{}]", m)
    client.execute {
      indexInto(delphiProjectType).fields("name" -> m.toUniqueString,
        "source" -> "Maven",
        "identifier" -> Map(
          "groupId" -> m.groupId,
          "artifactId" -> m.artifactId,
          "version" -> m.version))
    }.await
  }
}
