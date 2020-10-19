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

package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexMaintenance extends AppLogging {


  def createDelphiIndex(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")

    val client = ElasticClient(configuration.elasticsearchClientUri)
    val featureList = ElasticFeatureListMapping.getMapAsSeq

    val identifierFields = Seq(
      //Git
      textField("repoUrl"),
      keywordField("commitId"),

      //Maven
      textField("groupId"),
      textField("artifactId"),
      keywordField("version")
    )

    val f = client.execute {
      createIndex(delphi) mappings (
        mapping(project) as(
          keywordField("name"),
          keywordField("source"),
          keywordField("language"),
          dateField("discovered"),

          objectField("identifier") fields identifierFields,

          nestedField("calls") fields Seq(
            keywordField("name"),
            objectField("identifier") fields identifierFields,
            textField("methods") analyzer KeywordAnalyzer
          ),
          objectField("features") fields featureList
        ),
        mapping(processingError) as (
          keywordField("type"),
          keywordField("message"),
          dateField("occurred"),
          objectField("identifier") fields identifierFields
        ))
    }.await

    //Increases maximum number of nested fields
    client.execute {
      updateSettings(delphi).set(
        "index.mapping.nested_fields.limit", "250"
      )
    }.await

    Success(configuration)
  }

  def migrateIndex(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    Success(configuration)
  }

  def isIndexCurrent(configuration: Configuration)(implicit system: ActorSystem): Boolean = {
    true
  }
}
