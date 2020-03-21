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

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets

import de.upb.cs.swt.delphi.crawler.Identifier
import org.apache.commons.logging.LogFactory

case class MavenIdentifier(val repository: String, val groupId: String, val artifactId: String, val version: String) extends Identifier {
  def toUniqueString = {
    repository + ":" + groupId + ":" + artifactId + ":" + version
  }


  override val toString: String = groupId + ":" + artifactId + ":" + version

  def toJarLocation : URI = {
    constructArtifactBaseUri().resolve(encode(artifactId) + "-" + encode(version) + ".jar")
  }

  def toPomLocation : URI = {
    constructArtifactBaseUri().resolve(encode(artifactId) + "-" + encode(version) + ".pom")
  }

  private def constructArtifactBaseUri(): URI =
    new URI(repository)
      .resolve(encode(groupId).replace('.', '/') + "/")
      .resolve(encode(artifactId) + "/")
      .resolve(encode(version) + "/")

  private def encode(input : String) : String =
    URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
}

object MavenIdentifier {
  private val DefaultRepository = "https://repo1.maven.org/maven2/"

  private val log = LogFactory.getLog(MavenIdentifier.getClass)

  private implicit def wrapOption[A](value : A) : Option[A] = Some(value)

  def apply(s: String): Option[MavenIdentifier] = {
    if (!s.startsWith(DefaultRepository)) return None
    val identifier = s.replace(DefaultRepository + ":", "")
    val splitString: Array[String] = identifier.split(':')
    if (splitString.length < 2 || splitString.length > 3) return None

    MavenIdentifier(
      repository = DefaultRepository,
      groupId = splitString(0),
      artifactId = splitString(1),
      version = if (splitString.length < 3) "" else splitString(2))

  }
}