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

import java.io.ByteArrayInputStream

import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.joda.time.DateTime

case class MavenArtifact(identifier : MavenIdentifier, jarFile: JarFile, pomFile: PomFile, metadata: MavenArtifactMetadata)

case class MavenArtifactMetadata(publicationDate: DateTime, name: String, description: String)

object MavenArtifactMetadata {
  def readFromPom(pubDate: DateTime, pomFile: PomFile): Option[MavenArtifactMetadata] = {
    val pomReader: MavenXpp3Reader = new MavenXpp3Reader()

    val pomObj = pomReader.read(new ByteArrayInputStream(pomFile.content))

    Some(MavenArtifactMetadata(pubDate, pomObj.getName, pomObj.getDescription))
  }
}
