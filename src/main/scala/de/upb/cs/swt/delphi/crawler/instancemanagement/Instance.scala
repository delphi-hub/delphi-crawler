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
package de.upb.cs.swt.delphi.crawler.instancemanagement

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val componentTypeFormat : JsonFormat[InstanceEnums.ComponentType] = new JsonFormat[InstanceEnums.ComponentType] {

    def write(compType : InstanceEnums.ComponentType) = JsString(compType.toString)

    def read(value: JsValue) : InstanceEnums.ComponentType = value match {
      case JsString(s) => s match {
        case "Crawler" => InstanceEnums.ComponentType.Crawler
        case "WebApi" => InstanceEnums.ComponentType.WebApi
        case "WebApp" => InstanceEnums.ComponentType.WebApp
        case "DelphiManagement" => InstanceEnums.ComponentType.DelphiManagement
        case "ElasticSearch" => InstanceEnums.ComponentType.ElasticSearch
        case x => throw DeserializationException(s"Unexpected string value $x for component type.")
      }
      case y => throw DeserializationException(s"Unexpected type $y while deserializing component type.")
    }
  }

  implicit val stateFormat  : JsonFormat[InstanceEnums.State] = new JsonFormat[InstanceEnums.State] {

    def write(compType : InstanceEnums.State) = JsString(compType.toString)

    def read(value: JsValue) : InstanceEnums.State = value match {
      case JsString(s) => s match {
        case "Running" => InstanceEnums.InstanceState.Running
        case "Stopped" => InstanceEnums.InstanceState.Stopped
        case "Failed" => InstanceEnums.InstanceState.Failed
        case "Paused" => InstanceEnums.InstanceState.Paused
        case "NotReachable" => InstanceEnums.InstanceState.NotReachable
        case x => throw DeserializationException(s"Unexpected string value $x for instance state.")
      }
      case y => throw DeserializationException(s"Unexpected type $y while deserializing instance state.")
    }
  }

  implicit val instanceFormat : JsonFormat[Instance] = jsonFormat7(Instance)
}

final case class Instance (
                            id: Option[Long],
                            host: String,
                            portNumber: Long,
                            name: String,
                            componentType: InstanceEnums.ComponentType,
                            dockerId: Option[String],
                            instanceState: InstanceEnums.State
                          ) {}

object InstanceEnums {

  type ComponentType = ComponentType.Value
  object ComponentType extends Enumeration {
    val Crawler  : Value = Value("Crawler")
    val WebApi : Value = Value("WebApi")
    val WebApp : Value = Value("WebApp")
    val DelphiManagement : Value = Value("DelphiManagement")
    val ElasticSearch : Value = Value("ElasticSearch")
  }

  type State = InstanceState.Value
  object InstanceState extends Enumeration {
    val Running : Value = Value("Running")
    val Stopped : Value = Value("Stopped")
    val Failed : Value = Value("Failed")
    val Paused : Value = Value("Paused")
    val NotReachable : Value = Value("NotReachable")
  }

}