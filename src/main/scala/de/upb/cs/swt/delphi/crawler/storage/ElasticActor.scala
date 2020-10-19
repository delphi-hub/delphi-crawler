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
import akka.event.LoggingAdapter
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.Identifier
import de.upb.cs.swt.delphi.crawler.discovery.git.GitIdentifier
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}
import de.upb.cs.swt.delphi.crawler.discovery.maven.{MavenIdentifier, MavenProcessingError}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact
import de.upb.cs.swt.delphi.crawler.processing.{HermesResults, PomFileReadActorResponse}

/**
  * An actor reacting to item which should be pushed to elasticsearch
  * @param client The currently active elasticsearch client
  * @author Ben Hermann
  */
class ElasticActor(client: ElasticClient) extends Actor with ActorLogging with ArtifactIdentityQuery with ElasticStoreQueries {
  private implicit val c : ElasticClient = client
  private implicit val l : LoggingAdapter = log

  override def receive: PartialFunction[Any, Unit] = {
    case StreamInitialized =>
      log.info(s"Stream initialized!")
      sender() ! Ack
    case StreamCompleted =>
      log.info(s"Stream completed!")
    case StreamFailure(ex) =>
      log.error(ex, s"Stream failed!")
    case m : MavenIdentifier => {
      log.info(s"pushing $m")
      store(m)
      sender() ! Ack
    }
    case PomFileReadActorResponse(artifact,_,false,_) => {
      store(artifact)
      sender() ! Ack
    }
    case g : GitIdentifier => {
      store(g)
      sender() ! Ack
    }
    case h : HermesResults => {
      store(h)
      sender() ! Ack
    }
    case e : MavenProcessingError => {
      store(e)
      sender() ! Ack
    }
    case x => log.warning("Received unknown message: [{}] ", x)
  }

}

object ElasticActor {
  def props(client: ElasticClient): Props = Props(new ElasticActor(client))

  case class Push(identity: Identifier)

}





