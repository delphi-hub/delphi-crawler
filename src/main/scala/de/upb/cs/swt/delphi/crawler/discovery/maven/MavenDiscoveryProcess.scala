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
import akka.routing.{RoundRobinPool, SmallestMailboxPool}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticApi.RichFuture
import com.sksamuel.elastic4s.ElasticClient
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.model.ProcessingPhase.ProcessingPhase
import de.upb.cs.swt.delphi.crawler.model.{MavenArtifact, ProcessingError, ProcessingPhase, ProcessingPhaseFailedException}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActor
import de.upb.cs.swt.delphi.crawler.processing.{HermesActor, HermesResults, PomFileReadActor}
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}
import de.upb.cs.swt.delphi.crawler.tools.{ElasticHelper, NotYetImplementedException}
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps
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

  val downloaderPool: ActorRef =
    system.actorOf(SmallestMailboxPool(configuration.downloadActorPoolSize).props(MavenDownloadActor.props))

  val pomReaderPool: ActorRef =
    system.actorOf(RoundRobinPool(configuration.pomReadActorPoolSize).props(PomFileReadActor.props(configuration)))

  val hermesPool: ActorRef =
    system.actorOf(SmallestMailboxPool(configuration.hermesActorPoolSize).props(HermesActor.props()))

  override def phase: Phase = Phase.Discovery

  override def start: Try[Long] = {
    implicit val logger: LoggingAdapter = log
    implicit val client: ElasticClient =
      ElasticHelper.buildElasticClient(configuration)

    val identifierSource = buildThrottledSource()

    implicit val timeout: Timeout = Timeout(5 minutes)

    val preprocessing = buildPreprocessingPipeline(identifierSource)

    preprocessing
      .mapAsync(8)(artifact => (pomReaderPool ? artifact).mapTo[MavenArtifact])
      .filter{ artifact =>

        if(artifact.metadata.isEmpty){
          (elasticPool ? new ProcessingError(artifact.identifier, ProcessingPhase.PomProcessing,
          "Unexpected error while processing POM file.", None)).await
          false
        } else if(!checkValidPackaging(artifact)) {
          log.error(s"Invalid packaging for ${artifact.identifier.toUniqueString}")
          (elasticPool ? new ProcessingError(artifact.identifier, ProcessingPhase.PomProcessing,
            "JAR file not found although packaging is JAR", None)).await
          false
        } else {
          true
        }
      }
      .alsoTo(storageSink[MavenArtifact]())
      .mapAsync(8)(artifact => (hermesPool ? artifact).mapTo[Try[HermesResults]])
      .filter(isSuccessAndReportFailure(_, ProcessingPhase.MetricsExtraction))
      .map(_.get)
      .runWith(storageSink[HermesResults]())

    Success(0L)
  }

  private def buildThrottledSource()(implicit log: LoggingAdapter, client: ElasticClient): Source[MavenIdentifier, NotUsed] = {
    var source =
      createSource(configuration.mavenRepoBase)
        .filter(m => {
          val before = seen.contains(m)
          if (!before) seen.add(m)
          !before
        }) // local seen cache to compensate for lags
        .filter(m => !exists(m)) // ask elastic
        .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)

    if (configuration.limit > 0) {
      source = source.take(configuration.limit)
    }

    source
  }

  private def buildPreprocessingPipeline(source: Source[MavenIdentifier, _])(implicit timeout: Timeout): Source[MavenArtifact, _] = {
    source
      .alsoTo(storageSink[MavenIdentifier]())
      .mapAsync(8)(identifier => (downloaderPool ? identifier).mapTo[Try[MavenArtifact]])
      .filter(isSuccessAndReportFailure(_, ProcessingPhase.Downloading))
      .map(artifact => artifact.get)
  }

  private def checkValidPackaging(artifact: MavenArtifact): Boolean = {
    artifact.jarFile.isDefined ||
      artifact.metadata.isDefined && !artifact.metadata.get.packaging.equalsIgnoreCase("jar")
  }

  private def isSuccessAndReportFailure[T](element: Try[T], processingPhase: ProcessingPhase)
                                          (implicit timeout: Timeout) = element match {
    case Failure(ProcessingPhaseFailedException(ident, cause)) =>
      (elasticPool ? new ProcessingError(ident, processingPhase, cause.getMessage, Some(cause))).await
      false
    case Failure(ex) =>
      (elasticPool ? new ProcessingError(null, processingPhase, ex.getMessage, Some(ex))).await
      false
    case _ => true
  }

  private def storageSink[T](): Sink[T, NotUsed] = createSinkFromActorRef[T](elasticPool)

  private def createSinkFromActorRef[T](actorRef: ActorRef) = {
    Sink.actorRefWithAck[T](actorRef, StreamInitialized, Ack, StreamCompleted, StreamFailure)
  }

  override def stop: Try[Long] = {
    Failure(new NotYetImplementedException)
  }
}


