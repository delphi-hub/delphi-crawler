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

import java.net.URI

import de.upb.cs.swt.delphi.crawler.Identifier

case class MavenIdentifier(val repository: String, val groupId: String, val artifactId: String, val version: String) extends Identifier {
  def toUniqueString = {
    repository + ":" + groupId + ":" + artifactId + ":" + version
  }

  def toJarLocation : URI = {
    constructArtifactBaseUri().resolve(artifactId + "-" + version + ".jar")
  }

  def toPomLocation : URI = {
    constructArtifactBaseUri().resolve(artifactId + "-" + version + ".pom")
  }

  private def constructArtifactBaseUri(): URI =
    new URI(repository)
      .resolve(groupId.replace('.', '/') + "/")
      .resolve(artifactId + "/")
      .resolve(version + "/")
}
