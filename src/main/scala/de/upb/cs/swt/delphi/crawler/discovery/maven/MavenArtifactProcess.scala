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
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.routing.SmallestMailboxPool
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenDownloadActor, MavenDownloadActorResponse}
import de.upb.cs.swt.delphi.crawler.processing._
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery
import de.upb.cs.swt.delphi.crawler.tools.NotYetImplementedException
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


/**
  * A process to discover new artifacts from a configured maven repository.
  *
  * @author Ben Hermann, Johannes Düsing
  * @param configuration The configuration to be used
  * @param elasticPool   A pool of elasticsearch actors
  * @param system        The currently used actor system (for logging)
  *
  */
abstract class MavenArtifactProcess(configuration: Configuration,
                                    elasticPool: ActorRef)
                           (implicit system: ActorSystem)
  extends de.upb.cs.swt.delphi.crawler.control.Process[Long]
    with IndexProcessing
    with ArtifactExistsQuery
    with AppLogging {

  protected val identifierSource: Source[MavenIdentifier, NotUsed]

  protected implicit val esClient: ElasticClient = ElasticClient(configuration.elasticsearchClientUri)

  protected implicit val processingTimeout: Timeout = Timeout(5.minutes)

  protected implicit val logger: LoggingAdapter = log

  private val downloaderPool =
    system.actorOf(SmallestMailboxPool(8).props(MavenDownloadActor.props))
  private val pomReaderPool =
    system.actorOf(SmallestMailboxPool(8).props(PomFileReadActor.props(configuration)))
  private val errorHandlerPool =
    system.actorOf(SmallestMailboxPool(8).props(ProcessingFailureStorageActor.props(elasticPool)))
  private val hermesPool =
    system.actorOf(SmallestMailboxPool(configuration.hermesActorPoolSize).props(HermesActor.props()))

  override def phase: Phase = Phase.Discovery


  override def start: Try[Long] = {

    val filteredSource = if (configuration.limit > 0) {
      identifierSource.take(configuration.limit)
    } else {
      identifierSource
    }

    val preprocessing =
      filteredSource
        .alsoTo(createSinkFromActorRef[MavenIdentifier](elasticPool))
        .mapAsync(8)(identifier => (downloaderPool ? identifier).mapTo[MavenDownloadActorResponse])
        .alsoTo(createSinkFromActorRef[MavenDownloadActorResponse](errorHandlerPool))
        .filter(!_.pomDownloadFailed)

    val finalizer =
      preprocessing
        .mapAsync(8)(downloadResponse => (pomReaderPool ? downloadResponse).mapTo[PomFileReadActorResponse])
        .alsoTo(createSinkFromActorRef[PomFileReadActorResponse](errorHandlerPool))
        .alsoTo(createSinkFromActorRef[PomFileReadActorResponse](elasticPool))
        .filter(response => !response.jarDownloadFailed)
        .map(_.artifact)
        .mapAsync(configuration.hermesActorPoolSize)(artifact => (hermesPool ? artifact).mapTo[HermesActorResponse])
        .alsoTo(createSinkFromActorRef[HermesActorResponse](errorHandlerPool))
        .filter(_.result.isSuccess)
        .map(_.result.get)
        .alsoTo(createSinkFromActorRef[HermesResults](elasticPool))
        .to(Sink.ignore)
        .run()

    Success(0L)
  }

  private def createSinkFromActorRef[T](actorRef: ActorRef) = {
    Sink.actorRefWithAck[T](actorRef, StreamInitialized, Ack, StreamCompleted, StreamFailure)
  }

  override def stop: Try[Long] = {
    Failure(new NotYetImplementedException)
  }
}



