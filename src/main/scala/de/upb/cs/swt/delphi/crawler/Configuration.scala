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

package de.upb.cs.swt.delphi.crawler

import java.net.URI

import akka.stream.ThrottleMode
import com.sksamuel.elastic4s.ElasticsearchClientUri
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceEnums.{ComponentType, InstanceState}
import de.upb.cs.swt.delphi.crawler.instancemanagement.{Instance, InstanceLink, InstanceRegistry}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class Configuration {



  lazy val elasticsearchClientUri: String = elasticSearchInstance.host + ":" + elasticSearchInstance.portNumber

  lazy val elasticSearchInstance : Instance = InstanceRegistry.retrieveElasticSearchInstance(this) match {
    case Success(instance) => instance
    case Failure(_) => Instance(
      None,
      fallbackElasticSearchHost,
      fallbackElasticSearchPort,
      "Default ElasticSearch instance",
      ComponentType.ElasticSearch,
      None,
      InstanceState.Running,
      List.empty[String],
      List.empty[InstanceLink],
      List.empty[InstanceLink])
  }

  val mavenRepoBase: URI = new URI("https://repo1.maven.org/maven2/") // TODO: Create a local demo server "http://localhost:8881/maven2/"
  val controlServerPort : Int = 8882

  val defaultElasticSearchPort : Int = 9200
  val defaultElasticSearchHost : String = "localhost"

  lazy val fallbackElasticSearchPort : Int = sys.env.get("DELPHI_ELASTIC_URI") match {
    case Some(hostString) => if(hostString.count(c => c == ':') == 2){
        Try(hostString.split(":")(2).toInt) match {
          case Success(port) => port
          case Failure(_) => defaultElasticSearchPort
        }
      } else {
      defaultElasticSearchPort
    }
    case None => defaultElasticSearchPort
  }

  lazy val fallbackElasticSearchHost : String = sys.env.get("DELPHI_ELASTIC_URI") match {
    case Some(hostString) =>
      if(hostString.count(c => c == ':') == 2){
       hostString.substring(0,hostString.lastIndexOf(":"))
      } else {
        defaultElasticSearchHost
      }
    case None => defaultElasticSearchHost

  }
  val limit : Int = 0
  val throttle : Throttle = Throttle(5, 1 second, 5, ThrottleMode.shaping)

  val tempFileStorage : String = "temp/"

  val elasticActorPoolSize : Int = 4
  val callGraphStreamPoolSize : Int = 4
  val hermesActorPoolSize: Int = 2

  val downloadActorPoolSize: Int = 8
  val pomReadActorPoolSize: Int = 4




  val instanceName = "MyCrawlerInstance"

  val instanceRegistryUri : String = sys.env.getOrElse("DELPHI_IR_URI", "http://localhost:8087")

  lazy val usingInstanceRegistry : Boolean = instanceId.isDefined

  lazy val instanceId : Option[Long] = InstanceRegistry.handleInstanceStart(this)

  val jwtSecretKey: String  = sys.env.getOrElse("DELPHI_JWT_SECRET","changeme")

  case class Throttle(element : Int, per : FiniteDuration, maxBurst : Int, mode : ThrottleMode)
}

