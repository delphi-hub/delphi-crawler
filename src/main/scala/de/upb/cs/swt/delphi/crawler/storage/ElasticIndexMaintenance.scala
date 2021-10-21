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
import com.sksamuel.elastic4s.fields.ObjectField
import de.upb.cs.swt.delphi.crawler.tools.ElasticHelper
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexMaintenance extends AppLogging {

  import com.sksamuel.elastic4s.ElasticDsl._

  private val identifierFields = Seq(
    textField("groupId"),
    textField("artifactId"),
    keywordField("version")
  )


  def createDelphiIndex(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")

    val client = ElasticHelper.buildElasticClient(configuration)
    val featureList = ElasticFeatureListMapping.getMapAsSeq

    val f = client.execute {
      createIndex(identifierIndexName) mapping properties (
        keywordField("name"),
        keywordField("source"),
        dateField("discovered"),
        ObjectField(name = "identifier", properties = identifierFields)
      )
      createIndex(metadataIndexName) mapping properties (
        keywordField("name"),
        ObjectField(name = "identifier", properties = identifierFields),
        ObjectField(name = "parent", properties = identifierFields),
        keywordField("packaging"),
        textField("description"),
        textField("licenses"),
        textField("issueManagement"),
        textField("developers"),
        nestedField("dependencies").fields( identifierFields ++ Seq(keywordField("scope")) ),
        dateField("published")
      )
      createIndex(metricIndexName) mapping properties (
        keywordField("name"),
        ObjectField(name = "identifier", properties = identifierFields),
        ObjectField(name = "features", properties = featureList)
      )
      createIndex(errorIndexName) mapping properties (
        keywordField("name"),
        ObjectField(name = "identifier", properties = identifierFields),
        keywordField("phase"),
        textField("message")
      )
    }.await

    //Increases maximum number of nested fields
    client.execute {
      updateSettings(metricIndexName).set(
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
