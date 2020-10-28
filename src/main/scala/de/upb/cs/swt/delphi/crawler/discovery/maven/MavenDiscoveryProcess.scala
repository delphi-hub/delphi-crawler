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
import de.upb.cs.swt.delphi.crawler.Configuration
import scala.collection.mutable


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
                           (implicit system: ActorSystem)
  extends MavenArtifactProcess(configuration, elasticPool)(system) {

  private val seen = mutable.HashSet[MavenIdentifier]()

  override val identifierSource: Source[MavenIdentifier, NotUsed] =
    createSource(configuration.mavenRepoBase)
      .filter(m => {
        val before = seen.contains(m)
        if (!before) seen.add(m)
        !before
      }) // local seen cache to compensate for lags
      .filter(m => !exists(m)) // ask elastic
      .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)


}


