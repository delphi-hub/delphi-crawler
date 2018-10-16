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
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery
import de.upb.cs.swt.delphi.crawler.tools.NotYetImplementedException

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

/**
  * A process to discover new artifacts from a configured maven repository.
  *
  * @author Ben Hermann
  * @param configuration The configuration to be used
  * @param forwarder An actor that will be notified on discovered {@see MavenIdentifier} objects
  * @param system The currently used actor system (for logging)
  *
  */
class MavenDiscoveryProcess(configuration: Configuration, forwarder: ActorRef)
                           (implicit system: ActorSystem)
  extends de.upb.cs.swt.delphi.crawler.control.Process[Long]
    with IndexProcessing
    with ArtifactExistsQuery
    with AppLogging {

  private val seen = mutable.HashSet[MavenIdentifier]()

  override def phase: Phase = Phase.Discovery

  override def start: Try[Long] = {
    implicit val materializer = ActorMaterializer()
    implicit val logger: LoggingAdapter = log
    implicit val client = ElasticClient(configuration.elasticsearchClientUri)

    val (preproccess, count) =
      createSource(configuration.mavenRepoBase)
        .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)
        .filter(m => {
          val before = seen.contains(m)
          if (!before) seen.add(m)
          !before
        }) // local seen cache to compensate for lags
        .filter(m => !exists(m)) // ask elastic
        .take(configuration.limit) // limit for now
        .alsoToMat(Sink.foreach(forwarder ! _))(Keep.right)
        .toMat(Sink.fold(0L)((acc, _) => acc + 1))(Keep.both)
        .run()


    Try(Await.result(count, Duration.Inf))
  }

  override def stop: Try[Long] = {
    Failure(new NotYetImplementedException)
  }
}
