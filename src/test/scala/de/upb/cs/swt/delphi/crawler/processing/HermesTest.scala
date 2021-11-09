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

import java.io.BufferedInputStream
import java.util.jar.JarInputStream
import de.upb.cs.swt.delphi.crawler.tools.{ClassStreamReader, OPALLogAdapter}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should
import org.scalatest.BeforeAndAfterAll

import scala.reflect.io.File

class HermesTest extends AnyFlatSpecLike with should.Matchers with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    HermesAnalyzer.setConfig()
    OPALLogAdapter.setOpalLoggingEnabled(false)
  }

  "Hermes" should "run on sootconfig-1.1" in {
    val file = File("src/test/resources/sootconfig-1.1.jar")
    val fis = file.inputStream()
    val jis = new JarInputStream(new BufferedInputStream(fis))

    val p = ClassStreamReader.createProject(file.toURL, jis, projectIsLibrary = true)

    val result = new HermesAnalyzer(p).analyzeProject()
    for {
      (query, features) <- result
      feature <- features
    } {
      println(s"${query.id} - ${feature.id.replace("\n", " ")}: ${feature.count}")
    }
  }

  "HermesVersion" should "be a valid version string" in {
    HermesAnalyzer.HermesVersion shouldBe a[String]
    HermesAnalyzer.HermesVersion shouldBe "4.0.0"
  }


}