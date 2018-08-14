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

import java.net.URL
import java.util.jar.JarInputStream

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.opalj.hermes.{Feature, FeatureQuery}

class HermesActor(elasticActor : ActorRef) extends Actor with ActorLogging {


  def transformToFeatures(results: Iterator[(FeatureQuery, TraversableOnce[Feature[URL]])]) = {
    results.flatMap{ case(query, features: TraversableOnce[Feature[URL]]) => features.map { case feature => feature.id -> feature.count}} toMap
  }

  def receive = {
    case m : MavenArtifact => {
      log.info(s"Starting analysis for $m")
      val project = new ClassStreamReader{}.createProject(m.identifier.toJarLocation.toURL,
                                            new JarInputStream(m.jarFile.is))

      val results = new HermesAnalyzer(project).analyzeProject()

      val featureMap = transformToFeatures(results)

      val hermesResult = HermesResults(m.identifier, featureMap)

      log.info(s"Feature map for ${m.identifier.toUniqueString} is ${featureMap.size}.")
      elasticActor ! hermesResult
    }

  }
}

object HermesActor {
  type HermesStatistics = scala.collection.Map[String, Double]
  def props(elasticActor : ActorRef) = Props(new HermesActor(elasticActor))

}

case class HermesResults(identifier: MavenIdentifier, featureMap: Map[String, Int])