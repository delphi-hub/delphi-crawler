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
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceEnums.{ComponentType, InstanceState}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

/**
  * Trait defining the implicit JSON formats needed to work with Instances
  */
trait InstanceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with InstanceLinkJsonSupport {

  //Custom JSON format for an ComponentType
  implicit val componentTypeFormat : JsonFormat[ComponentType] = new JsonFormat[ComponentType] {

    /**
      * Custom write method for serializing an ComponentType
      * @param compType The ComponentType to serialize
      * @return JsString containing the serialized value
      */
    def write(compType : ComponentType) = JsString(compType.toString)

    /**
      * Custom read method for deserialization of an ComponentType
      * @param value JsValue to deserialize (must be a JsString)
      * @return ComponentType that has been read
      * @throws DeserializationException Exception thrown when JsValue is in incorrect format
      */
    def read(value: JsValue) : ComponentType = value match {
      case JsString(s) => s match {
        case "Crawler" => ComponentType.Crawler
        case "WebApi" => ComponentType.WebApi
        case "WebApp" => ComponentType.WebApp
        case "DelphiManagement" => ComponentType.DelphiManagement
        case "ElasticSearch" => ComponentType.ElasticSearch
        case x => throw DeserializationException(s"Unexpected string value $x for component type.")
      }
      case y => throw DeserializationException(s"Unexpected type $y during deserialization component type.")
    }
  }

  //Custom JSON format for an InstanceState
  implicit val stateFormat  : JsonFormat[InstanceState] = new JsonFormat[InstanceState] {

    /**
      * Custom write method for serializing an InstanceState
      * @param state The InstanceState to serialize
      * @return JsString containing the serialized value
      */
    def write(state : InstanceState) = JsString(state.toString)

    /**
      * Custom read method for deserialization of an InstanceState
      * @param value JsValue to deserialize (must be a JsString)
      * @return InstanceState that has been read
      * @throws DeserializationException Exception thrown when JsValue is in incorrect format
      */
    def read(value: JsValue) : InstanceState = value match {
      case JsString(s) => s match {
        case "Running" => InstanceState.Running
        case "Stopped" => InstanceState.Stopped
        case "Failed" => InstanceState.Failed
        case "Paused" => InstanceState.Paused
        case "NotReachable" => InstanceState.NotReachable
        case "Deploying" => InstanceState.Deploying
        case x => throw DeserializationException(s"Unexpected string value $x for instance state.")
      }
      case y => throw DeserializationException(s"Unexpected type $y during deserialization instance state.")
    }
  }

  //JSON format for Instances
  implicit val instanceFormat : JsonFormat[Instance] = jsonFormat10(Instance)
}

/**
  * The instance type used for transmitting data about an instance from an to the registry
  * @param id Id of the instance. This is an Option[Long], as an registering instance will not yet have an id.
  * @param host Host of the instance.
  * @param portNumber Port the instance is reachable at.
  * @param name Name of the instance
  * @param componentType ComponentType of the instance.
  * @param dockerId The docker container id of the instance. This is an Option[String], as not all instance have to be docker containers.
  * @param instanceState State of the instance
  */
final case class Instance (
                            id: Option[Long],
                            host: String,
                            portNumber: Long,
                            name: String,
                            componentType: ComponentType,
                            dockerId: Option[String],
                            instanceState: InstanceState,
                            labels: List[String],
                            linksTo: List[InstanceLink],
                            linksFrom: List[InstanceLink]
                          )

/**
  * Enumerations concerning instances
  */
object InstanceEnums {

  //Type to use when working with component types
  type ComponentType = ComponentType.Value

  /**
    * ComponentType enumeration defining the valid types of delphi components
    */
  object ComponentType extends Enumeration {
    val Crawler  : Value = Value("Crawler")
    val WebApi : Value = Value("WebApi")
    val WebApp : Value = Value("WebApp")
    val DelphiManagement : Value = Value("DelphiManagement")
    val ElasticSearch : Value = Value("ElasticSearch")
  }

  //Type to use when working with instance states
  type InstanceState = InstanceState.Value

  /**
    * InstanceState enumeration defining the valid states for instances of delphi components
    */
  object InstanceState extends Enumeration {
    val Deploying : Value = Value("Deploying")
    val Running : Value = Value("Running")
    val Stopped : Value = Value("Stopped")
    val Failed : Value = Value("Failed")
    val Paused : Value = Value("Paused")
    val NotReachable : Value = Value("NotReachable")
  }

}