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

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenCrawlActor.Start
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery

import scala.collection.mutable

class MavenCrawlActor(configuration: Configuration, nextStep : ActorRef)
                                                    extends Actor
                                                    with ActorLogging
                                                    with IndexProcessing
                                                    with ArtifactExistsQuery {
  val seen = mutable.HashSet[MavenIdentifier]()

  override def receive: Receive = {
    case Start => {
      log.info("Starting Maven discovery process...")

      implicit val materializer = ActorMaterializer()
      implicit val client = HttpClient(configuration.elasticsearchClientUri)
      implicit val logger = log

      createSource(configuration.mavenRepoBase)
        .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)
        .filter(m => {
          val before = seen.contains(m)
          if (!before) seen.add(m)
          !before
        }) // local seen cache to compensate for lags
        .filter(m => !exists(m))  // ask elastic
        .take(configuration.limit) // limit for now
        .runForeach(m => {
          nextStep ! m
        } ) // TODO: instead as runForeach we might consider using the next step actor as a sink


    }
  }
}

object MavenCrawlActor {
  def props(configuration: Configuration, nextStep : ActorRef) = Props(new MavenCrawlActor(configuration, nextStep))

  case object Start
}
