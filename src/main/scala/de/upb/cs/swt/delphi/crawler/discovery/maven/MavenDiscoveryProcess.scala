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
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.ask
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActorResponse
import de.upb.cs.swt.delphi.crawler.processing.{HermesActorResponse, HermesResults, PomFileReadActorResponse}
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}
import de.upb.cs.swt.delphi.crawler.tools.NotYetImplementedException

import scala.collection.mutable
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
class MavenDiscoveryProcess(configuration: Configuration, elasticPool: ActorRef)
                           (implicit system: ActorSystem) extends MavenProcess(configuration, elasticPool)(system)
  with IndexProcessing
  with ArtifactExistsQuery {

  private val seen = mutable.HashSet[MavenIdentifier]()

  private val identifierSource: Source[MavenIdentifier, NotUsed] =
    createSource(configuration.mavenRepoBase)
      .filter(m => {
        val before = seen.contains(m)
        if (!before) seen.add(m)
        !before
      }) // local seen cache to compensate for lags
      .filter(m => !exists(m)) // ask elastic
      .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)


  override def start: Try[Long] = {

    val filteredSource = if (configuration.limit > 0) identifierSource.take(configuration.limit) else identifierSource

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


