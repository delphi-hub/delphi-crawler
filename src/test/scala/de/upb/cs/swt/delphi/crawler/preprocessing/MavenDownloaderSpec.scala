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

import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader
import org.scalatest.{FlatSpec, Matchers}
import de.upb.cs.swt.delphi.crawler.preprocessing.Common._

class MavenDownloaderSpec extends FlatSpec with Matchers {
  "MavenDownloader" should "save jar file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val jarStream = downloader.downloadJar()
    checkJar(jarStream.is)
  }
  "MavenDownloader" should "save pom file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val pomStream = downloader.downloadPom()
    checkPom(pomStream.is)
  }

}
