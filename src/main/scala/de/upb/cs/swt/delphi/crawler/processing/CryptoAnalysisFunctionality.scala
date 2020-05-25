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

import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenLocalDownload}

import scala.collection.JavaConverters._
import java.io.File
trait CryptoAnalysisFunctionality {

  val tmpDir: String = System.getProperty("java.io.tmpdir")

  def computeCryptoAnalysisResult(m: MavenArtifact): CryptoAnalysisResult = {

    val tempFile : File = File.createTempFile(m.identifier.artifactId + "-" + m.identifier.version,".jar",new File(tmpDir))
    val applicationCp = MavenLocalDownload.tempDownloadJar(m, tempFile)
    val analyzer = new CryptoAnalyzer(m, applicationCp)
    analyzer.createScanner().exec()
    tempFile.delete()
    val cryptoErrorMap : Map[String,Integer] = crypto.reporting.CryptoDelphiErrorListener.getError.asScala.toMap
    val cryptoAnalysisResult = CryptoAnalysisResult(m.identifier, cryptoErrorMap)
    cryptoAnalysisResult
  }
}
