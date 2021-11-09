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
package de.upb.cs.swt.delphi.crawler.model

import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.model.ProcessingPhase.ProcessingPhase

case class ProcessingError(identifier: MavenIdentifier, phase: ProcessingPhase, message: String, cause: Option[Throwable])

object ProcessingError {

  def newDownloadError(ident: MavenIdentifier, cause: Throwable): ProcessingError = {
    new ProcessingError(ident, ProcessingPhase.Downloading, cause.getMessage, Some(cause))
  }

  def newPomProcessingError(ident: MavenIdentifier, cause: Throwable): ProcessingError = {
    new ProcessingError(ident, ProcessingPhase.PomProcessing, cause.getMessage, Some(cause))
  }

  def newMetricsExtractionError(ident: MavenIdentifier, cause: Throwable): ProcessingError = {
    new ProcessingError(ident, ProcessingPhase.MetricsExtraction, cause.getMessage, Some(cause))
  }

}

object ProcessingPhase extends Enumeration {
  type ProcessingPhase = Value

  val Downloading, PomProcessing, MetricsExtraction = Value
}

case class ProcessingPhaseFailedException(ident: MavenIdentifier, cause: Throwable) extends Throwable {
  override def getMessage: String = s"Processing of ${ident.toString} failed: ${cause.getMessage}"

  override def printStackTrace(): Unit = cause.printStackTrace()
}
