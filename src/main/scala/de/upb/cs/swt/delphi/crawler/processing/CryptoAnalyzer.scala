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

import java.io.File
import java.util
import crypto.analysis.CrySLRulesetSelector.RuleFormat
import crypto.HeadlessCryptoScanner
import crypto.analysis.CrySLRulesetSelector
import crypto.rules.CrySLRule
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact}
import soot.G
import crypto.HeadlessCryptoScanner.CG

class CryptoAnalyzer (m: MavenArtifact, applicationCp: String) {

  def createScanner(): HeadlessCryptoScanner = {
    G.v()
    G.reset()

    val resourcesPath: File = new java.io.File(".\\project\\JavaCryptographicArchitecture")
    print("resourcePath: " + resourcesPath + "\n")

    val scanner = new HeadlessCryptoScanner() {

      override def applicationClassPath(): String = applicationCp

      override def callGraphAlogrithm: HeadlessCryptoScanner.CG = CG.CHA

      override def getRules: util.List[CrySLRule] =
        CrySLRulesetSelector.makeFromPath(resourcesPath, RuleFormat.SOURCE)

      override def providerDetection: Boolean = false
    }
    scanner
  }

}
