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

import akka.actor.{Actor, ActorLogging, Props}
import scala.collection.Map
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact

import scala.util.Try


class CryptoAnalysisActor() extends Actor with ActorLogging with OPALFunctionality with CryptoAnalysisFunctionality {

  def receive: PartialFunction[Any, Unit] = {
    case m : MavenArtifact => {
      log.info(s"Starting CryptoAnalysis for $m")
      val cryptoAnalysisResult = Try {
        computeCryptoAnalysisResult(m)
      }
      sender() ! cryptoAnalysisResult
    }
  }

}

object CryptoAnalysisActor{
  def props(): Props = Props(new CryptoAnalysisActor)
}

case class CryptoAnalysisResult(identifier: MavenIdentifier, cryptoErrorMap: Map[String, Integer])