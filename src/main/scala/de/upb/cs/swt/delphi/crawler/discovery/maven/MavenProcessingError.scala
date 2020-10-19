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
}