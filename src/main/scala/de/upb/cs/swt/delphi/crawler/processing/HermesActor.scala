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

package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.model.{MavenArtifact, ProcessingPhaseFailedException}
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader

import scala.util.{Failure, Success, Try}

class HermesActor() extends Actor with ActorLogging with HermesFunctionality {

  def receive: PartialFunction[Any, Unit] = {
    case m : MavenArtifact => {
      log.info(s"Starting analysis for ${m.identifier.toString}")

      val hermesResult = Try {
        computeHermesResult(m, initializeOpalProject(m))
      }

      hermesResult match {
        case Success(r) =>
          sender() ! hermesResult
        case Failure(ex) =>
          log.error(ex, s"Hermes run failed for ${m.identifier}")
          sender() ! Failure(ProcessingPhaseFailedException(m.identifier, ex))
      }


    }
  }

  private def initializeOpalProject(artifact: MavenArtifact) = {
    val project = ClassStreamReader.createProject(artifact.identifier.toJarLocation.toURL,
      artifact.jarFile.get.is, true)
    Try(artifact.jarFile.get.is.close())
    project
  }
}

object HermesActor {
  type HermesStatistics = scala.collection.Map[String, Double]
  def props(): Props = Props(new HermesActor)

}

case class HermesResults(identifier: MavenIdentifier, featureMap: Map[String, Int])