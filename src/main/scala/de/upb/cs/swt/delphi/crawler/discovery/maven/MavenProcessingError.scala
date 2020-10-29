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

package de.upb.cs.swt.delphi.crawler.discovery.maven

import org.joda.time.DateTime

case class MavenProcessingError(identifier: MavenIdentifier,
                                occurredAt: DateTime,
                                errorType: MavenErrorType.Value,
                                message: String)

object MavenErrorType extends Enumeration {
  type MavenErrorType = Value

  val PomDownloadFailed, JarDownloadFailed, PomParsingFailed, HermesProcessingFailed = Value
}


object MavenProcessingError {

  private def createError(identifier: MavenIdentifier, errorType: MavenErrorType.Value, message: String) =
    MavenProcessingError(identifier, DateTime.now(), errorType, message)

  def createPomDownloadError(identifier: MavenIdentifier, message: String): MavenProcessingError =
    createError(identifier, MavenErrorType.PomDownloadFailed, message)

  def createJarDownloadError(identifier: MavenIdentifier, message: String): MavenProcessingError =
    createError(identifier, MavenErrorType.JarDownloadFailed, message)

  def createPomParsingError(identifier: MavenIdentifier, message: String): MavenProcessingError =
    createError(identifier, MavenErrorType.PomParsingFailed, message)

  def createHermesProcessingError(identifier: MavenIdentifier, message: String): MavenProcessingError =
    createError(identifier, MavenErrorType.HermesProcessingFailed, message)

  def toElasticSource(error: MavenProcessingError): Map[String, AnyRef] = {
    Map(
      "identifier" -> Map(
        "repo" -> error.identifier.repository,
        "groupId" -> error.identifier.groupId,
        "artifactId" -> error.identifier.artifactId,
        "version" -> error.identifier.version
      ),
      "occurred" -> error.occurredAt,
      "message" -> error.message,
      "type" -> error.errorType.toString
    )
  }

  def fromElasticSource(map: Map[String, AnyRef]): MavenProcessingError = {
    val identifier = identifierFromMap(map("identifier").asInstanceOf[Map[String, String]])
    val errorType = MavenErrorType.withName(map("type").asInstanceOf[String])
    val errorMessage = map("message").asInstanceOf[String]
    val errorTime = DateTime.parse(map("occurred").asInstanceOf[String])

    MavenProcessingError(identifier, errorTime, errorType, errorMessage)
  }

  private def identifierFromMap(map: Map[String, String]): MavenIdentifier =
    MavenIdentifier(map("repo"), map("groupId"), map("artifactId"), map("version"))
}