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
import de.upb.cs.swt.delphi.crawler.model.MavenArtifact

import java.net.URL
import org.opalj.br.analyses.Project
import org.opalj.hermes.{Feature, FeatureQuery}

trait HermesFunctionality {

  def transformToFeatures(results: Iterator[(FeatureQuery, TraversableOnce[Feature[URL]])]): Map[String, Int] = {
    results.flatMap { case (query, features: TraversableOnce[Feature[URL]]) => features.map { case feature => feature.id -> feature.count } } toMap
  }

  def computeHermesResult(m: MavenArtifact, project: Project[URL]): HermesResults = {
    val results = new HermesAnalyzer(project).analyzeProject()
    val featureMap = transformToFeatures(results)
    val hermesResult = HermesResults(m.identifier, featureMap)
    hermesResult
  }
}
