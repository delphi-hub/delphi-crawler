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

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.routing.SmallestMailboxPool
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Keep, Sink}
import akka.util.Timeout
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenArtifactMetadata, MavenDownloadActor}
import de.upb.cs.swt.delphi.crawler.processing.{HermesActor, HermesResults, PomFileReadActor}
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery
import de.upb.cs.swt.delphi.crawler.tools.NotYetImplementedException

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


/**
  * A process to discover new artifacts from a configured maven repository.
  *
  * @author Ben Hermann
  * @param configuration The configuration to be used
  * @param elasticPool   A pool of elasticsearch actors
  * @param system        The currently used actor system (for logging)
  *
  */
class MavenDiscoveryProcess(configuration: Configuration, elasticPool: ActorRef)
                           (implicit system: ActorSystem)
  extends de.upb.cs.swt.delphi.crawler.control.Process[Long]
    with IndexProcessing
    with ArtifactExistsQuery
    with AppLogging {

  private val seen = mutable.HashSet[MavenIdentifier]()

  val downloaderPool = system.actorOf(SmallestMailboxPool(8).props(MavenDownloadActor.props))
  val pomReaderPool = system.actorOf(SmallestMailboxPool(8).props(PomFileReadActor.props))
  val hermesPool = system.actorOf(SmallestMailboxPool(configuration.hermesActorPoolSize).props(HermesActor.props()))

  override def phase: Phase = Phase.Discovery

  override def start: Try[Long] = {
    implicit val materializer = ActorMaterializer()
    implicit val logger: LoggingAdapter = log
    implicit val client = ElasticClient(configuration.elasticsearchClientUri)

    var filteredSource =
      createSource(configuration.mavenRepoBase)
        .filter(m => {
          val before = seen.contains(m)
          if (!before) seen.add(m)
          !before
        }) // local seen cache to compensate for lags
        .filter(m => !exists(m)) // ask elastic
        .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)


    if (configuration.limit > 0) {
      filteredSource = filteredSource.take(configuration.limit)
    }

    implicit val timeout = Timeout(5 minutes)

    val preprocessing =
      filteredSource
        .alsoTo(createSinkFromActorRef[MavenIdentifier](elasticPool))
        .mapAsync(8)(identifier => (downloaderPool ? identifier).mapTo[Try[MavenArtifact]])
        .filter(artifact => artifact.isSuccess)
        .map(artifact => artifact.get)

    val finalizer =
      preprocessing
        .mapAsync(8)(artifact => (pomReaderPool ? artifact).mapTo[MavenArtifact])
        .mapAsync(configuration.hermesActorPoolSize)(artifact => (hermesPool ? artifact).mapTo[Try[HermesResults]])
        .filter(results => results.isSuccess)
        .map(results => results.get)
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


