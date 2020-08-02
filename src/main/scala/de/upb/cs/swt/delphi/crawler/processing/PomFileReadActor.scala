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

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.preprocessing.{ArtifactLicense, IssueManagementData, MavenArtifact, MavenArtifactMetadata, PomFile}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * An Actor that receives MavenArtifacts and extracts metadata from its POM file. If successful, an
  * MavenMetadata object is attached to the artifact and the artifact is returned. If failures occurr,
  * the artifact is returned without metadata.
  *
  * @author Johannes Düsing
  */
class PomFileReadActor extends Actor with ActorLogging{

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()

  override def receive: Receive = {
    case artifact@MavenArtifact(identifier, _ ,PomFile(pomStream), _, _) =>

      val pomObject = Try(pomReader.read(pomStream))
      pomStream.close()

      pomObject match {
        case Success(pom) =>

          val issueManagement = if (pom.getIssueManagement != null) {
            Some(IssueManagementData(pom.getIssueManagement.getSystem, pom.getIssueManagement.getUrl))
          } else {
            None
          }



          val metadata = MavenArtifactMetadata(pom.getName,
            pom.getDescription,
            pom.getDevelopers.asScala.map(_.getId).toList,
            pom.getLicenses.asScala.map(l => ArtifactLicense(l.getName, l.getUrl)).toList,
            issueManagement)

          sender() ! Success(MavenArtifact.withMetadata(artifact, metadata))

        case Failure(ex) =>
          log.error(s"Failed to parse POM file for artifact $identifier",ex )
          // Best effort semantics: If parsing fails, artifact is returned without metadata
          sender() ! artifact
      }

  }
}

object PomFileReadActor {
  def props: Props = Props(new PomFileReadActor)
}
