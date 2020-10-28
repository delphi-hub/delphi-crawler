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

package de.upb.cs.swt.delphi.crawler.discovery.maven

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.http.search.SearchResponse
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.storage.{delphi, processingError}

class MavenErrorRetryProcess(configuration: Configuration, elasticPool: ActorRef)(implicit system: ActorSystem)
  extends MavenArtifactProcess(configuration, elasticPool)(system) {

  override protected val identifierSource: Source[MavenIdentifier, NotUsed] = esClient.execute {
    search(delphi).types(processingError)
  }.await match {
    case f: RequestFailure =>
      logger.error(s"Failed to retrieve errors from elastic ${f.error.reason}")
      Source.empty[MavenIdentifier]
    case response: RequestSuccess[SearchResponse] =>
      Source.fromIterator(() => response
        .result
        .hits
        .hits
        .map{ hit =>
          val identifierMap = hit.fields("identifier").asInstanceOf[Map[String, String]]
          MavenIdentifier(configuration.mavenRepoBase.toString,
            identifierMap("groupId"),
            identifierMap("artifactId"),
            identifierMap("version")
          )
        }.iterator
      )
    case r@_ =>
      logger.error(s"Got unknown elastic response while querying errors: $r")
      Source.empty[MavenIdentifier]
  }

}
