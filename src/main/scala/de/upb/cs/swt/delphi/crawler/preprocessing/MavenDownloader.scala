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

package de.upb.cs.swt.delphi.crawler.preprocessing

import java.io.BufferedInputStream
import java.net.URI

import de.upb.cs.swt.delphi.crawler.discovery.maven.{HttpResourceHandler, MavenIdentifier}

class MavenDownloader(identifier: MavenIdentifier) {
  val http = new HttpResourceHandler(constructArtifactBaseUri())
  val pomResource = http.locate(pomFilename(identifier))
  val jarResource = http.locate(jarFilename(identifier))

  /**
    * Construct url from maven identifier
    * @return Base URI
    */
  def constructArtifactBaseUri(): URI =
    new URI(identifier.repository)
      .resolve(identifier.groupId.replace('.', '/') + "/")
      .resolve(identifier.artifactId + "/")
      .resolve(identifier.version + "/")


  def pomFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".pom"

  def jarFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".jar"

  def downloadJar(): JarFile = {
    JarFile(new BufferedInputStream(jarResource.read()))
  }

  def downloadPom(): PomFile= {
    PomFile(new BufferedInputStream(pomResource.read()))
  }
}
