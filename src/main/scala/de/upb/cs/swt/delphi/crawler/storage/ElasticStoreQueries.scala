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
import de.upb.cs.swt.delphi.crawler.discovery.git.GitIdentifier
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

  def store(g: GitIdentifier)(implicit client: ElasticClient, log: LoggingAdapter): Response[IndexResponse] = {
    log.info("Pushing new git identifier to elastic: [{}]", g)
    client.execute {
      indexInto(identifierIndexName).fields("name" -> (g.repoUrl + "/" + g.commitId),
        "source" -> "Git",
        "identifier" -> Map(
          "repoUrl" -> g.repoUrl,
          "commitId" -> g.commitId))
    }.await
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
}
