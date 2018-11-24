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

import LinkEnums.LinkState
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

trait InstanceLinkJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val linkStateFormat: JsonFormat[LinkState] = new JsonFormat[LinkState] {
    override def read(value: JsValue): LinkState = value match {
      case JsString(s) => s match {
        case "Assigned" => LinkState.Assigned
        case "Outdated" => LinkState.Outdated
        case "Failed" => LinkState.Failed
        case x => throw DeserializationException(s"Unexpected string value $x for LinkState.")
      }
      case y => throw DeserializationException(s"Unexpected type $y during deserialization of LinkState")
    }

    override def write(linkState: LinkState): JsValue = JsString(linkState.toString)
  }

  implicit val instanceLinkFormat: JsonFormat[InstanceLink] =
    jsonFormat3(InstanceLink)
}


final case class InstanceLink(idFrom: Long, idTo:Long, linkState: LinkState)

object LinkEnums {
  type LinkState = LinkState.Value

  object LinkState extends Enumeration {
    val Assigned: Value =  Value("Assigned")
    val Failed: Value = Value("Failed")
    val Outdated: Value = Value("Outdated")
  }
}
