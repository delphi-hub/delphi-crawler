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

package de.upb.cs.swt.delphi.crawler.tools

import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.discovery.maven.HttpResourceHandler
import de.upb.cs.swt.delphi.crawler.model.{JarFile, MetaFile, PomFile}

import java.net.{URI, URL}

/**
  * Http downloader that does not require any actor system, but instead uses Apaches Maven Resource interface to
  * process HTTP requests. Only used in tests.
  *
  * @param identifier Identifier to download
  */
class MavenDownloader(identifier: MavenIdentifier) {
  val http = new HttpResourceHandler(constructArtifactBaseUri())
  val pomResource = http.locate(pomFilename(identifier))
  val jarResource = http.locate(jarFilename(identifier))
  val metaResource = http.locate("maven-metadata.xml")

  /**
    * Construct url from maven identifier
    * @return Base URI
    */
  def constructArtifactBaseUri(): URI =
    new URI(identifier.repository.get)
      .resolve(identifier.groupId.replace('.', '/') + "/")
      .resolve(identifier.artifactId + "/")
      // .resolve(identifier.version + "/")

  def constructArtifactUrl(): URL =
    constructArtifactBaseUri().resolve(jarFilename(identifier)).toURL

  def pomFilename(identifier: MavenIdentifier): String =
    identifier.version.get + "/" + identifier.artifactId + "-" + identifier.version.get + ".pom"

  def jarFilename(identifier: MavenIdentifier): String =
    identifier.version.get + "/" + identifier.artifactId + "-" + identifier.version.get + ".jar"

  def downloadJar(): JarFile = {
    JarFile(jarResource.read(), constructArtifactUrl())
  }

  def downloadPom(): PomFile = {
    PomFile(pomResource.read())
  }

  def downloadMeta(): MetaFile = {
    MetaFile(metaResource.read())
  }
}
