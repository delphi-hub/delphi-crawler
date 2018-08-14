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

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, HttpClient}
import de.upb.cs.swt.delphi.crawler.Identifier
import de.upb.cs.swt.delphi.crawler.discovery.git.GitIdentifier
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.HermesResults

class ElasticActor(client: ElasticClient) extends Actor with ActorLogging with ArtifactIdentityQuery {

  override def receive = {
    case m : MavenIdentifier => {
      log.info("Pushing new maven identifier to elastic: [{}]", m)
      client.execute {
        indexInto(delphiProjectType).fields( "name" -> m.toUniqueString,
                                                     "source" -> "Maven",
                                                     "identifier" -> Map(
                                                       "groupId" -> m.groupId,
                                                       "artifactId" -> m.artifactId,
                                                       "version" -> m.version))
      }.await
    }
    case g : GitIdentifier => {
      log.info("Pushing new git identifier to elastic: [{}]", g)
      client.execute {
        indexInto(delphiProjectType).fields("name" -> (g.repoUrl +"/"+ g.commitId),
                                                     "source" -> "Git",
                                                     "identifier" -> Map(
                                                       "repoUrl" -> g.repoUrl,
                                                       "commitId" -> g.commitId))
      }
    }
    case h : HermesResults => {

      implicit val c = client
      elasticId(h.identifier) match {
        case Some(id) => {
          log.info(s"Pushing Hermes results for ${h.identifier} under id $id.")
          client.execute {
            update(id).in(delphiProjectType).doc("features" -> h.featureMap)
          }.await
        }
        case None => log.warning(s"Tried to push hermes results for non-existing identifier: ${h.identifier}.")
      }
    }
    case x => log.warning("Received unknown message: [{}] ", x)
  }
}

object ElasticActor {
  def props(client: ElasticClient): Props = Props(new ElasticActor(client))

  case class Push(identity: Identifier)

}





