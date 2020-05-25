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
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenDownloadActor, MavenLocalDownload}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Success
import scala.collection.JavaConverters._
/**
  * @author Seena Mathew
  */
class CryptoAnalyzerTest extends TestKit(ActorSystem("DownloadActor"))
with ImplicitSender
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll {
  TestKit.shutdownActorSystem (system)
  }

  "The Crypto Analyzer" must {
    "analyze the maven artifact for cryptographic errors based on JCA rules" in {
      val tmpDir: String = System.getProperty("java.io.tmpdir")
      //val mavenIdentifier = new MavenIdentifier("https://repo1.maven.org/maven2/", "org.apache.clerezza.ext", "javax.mail", "0.4-incubating")
      val mavenIdentifier = new MavenIdentifier("https://repo1.maven.org/maven2/", "org.web3j", "crypto", "4.5.17")
      val downloadActor = system.actorOf(MavenDownloadActor.props)

      implicit val timeout = Timeout(10 seconds)
      implicit val ec = system.dispatcher

      val f = downloadActor ? mavenIdentifier
      val msg = Await.result(f, 10 seconds)
      assert(msg.isInstanceOf[Success[MavenArtifact]])

      val artifact = msg.asInstanceOf[Success[MavenArtifact]].get
      val tempFile : File = File.createTempFile(artifact.identifier.artifactId + "-" + artifact.identifier.version,".jar",new File(tmpDir))
      val applicationCpCheck = MavenLocalDownload.tempDownloadJar(artifact, tempFile)
      printf("\nChecking applicationCpCheck: " + applicationCpCheck + "\n")
      val result = new CryptoAnalyzer(artifact, applicationCpCheck).createScanner().exec()
      tempFile.delete()
      val cryptoAnalysisResult = crypto.reporting.CryptoDelphiErrorListener.getError
      val sMap = cryptoAnalysisResult.asScala
      printf("Analysis Result from CryptoAnalysis to Delphi\n")
      for ((k,v) <- sMap)
        printf("%s, %s\n", k, v)

    }
  }
}

