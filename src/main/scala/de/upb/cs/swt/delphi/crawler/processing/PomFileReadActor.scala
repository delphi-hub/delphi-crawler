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

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.squareup.tools.maven.resolution.ArtifactResolver
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.model.{ArtifactDependency, ArtifactLicense, IssueManagementData, MavenArtifact, MavenArtifactMetadata, PomFile}
import de.upb.cs.swt.delphi.crawler.tools.HttpDownloader
import org.apache.maven.model.{Dependency, Model}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * An Actor that receives MavenArtifacts and extracts metadata from its POM file. If successful, an
  * MavenMetadata object is attached to the artifact and the artifact is returned. If failures occur,
  * the artifact is returned without metadata.
  *
  * @author Johannes Düsing
  */
class PomFileReadActor(configuration: Configuration) extends Actor with ActorLogging{

  val theResolver = new ArtifactResolver()

  implicit val system : ActorSystem = context.system

  override def receive: Receive = {
    case artifact@MavenArtifact(identifier, PomFile(pomStream), _, _, _) =>

      pomStream.close()

      Try{
        val theArtifact = theResolver.artifactFor(s"${identifier.groupId}:${identifier.artifactId}:${identifier.version.get}")
        val resolvedArtifact = theResolver.resolve(theArtifact)
        resolvedArtifact.getArtifact.getModel
      } match {
        case Success(pom) =>
          val issueManagement = Option(pom.getIssueManagement)
            .map(i => IssueManagementData(i.getSystem, i.getUrl))

          val parent = Option(pom.getParent)
            .map(p => MavenIdentifier(Some(configuration.mavenRepoBase.toString), p.getGroupId, p.getArtifactId, Some(p.getVersion)))

          val dependencies = pom.getDependencies.asScala.map { d =>
            ArtifactDependency(
              new MavenIdentifier(Some(configuration.mavenRepoBase.toString), d.getGroupId, d.getArtifactId, Some(d.getVersion)),
              Option(d.getScope))
          }.toSet
          val metadata = MavenArtifactMetadata(pom.getName,
            pom.getDescription,
            pom.getDevelopers.asScala.map(_.getId).toList,
            pom.getLicenses.asScala.map(l => ArtifactLicense(l.getName, l.getUrl)).toList,
            issueManagement,
            dependencies,
            parent,
            pom.getPackaging)

          sender() ! MavenArtifact.withMetadata(artifact, metadata)

        case Failure(ex) =>
          log.error(s"Failed to parse POM file for artifact $identifier",ex )
          // Best effort semantics: If parsing fails, artifact is returned without metadata
          sender() ! artifact
      }
  }
}

object PomFileReadActor {
  def props(configuration: Configuration):Props = Props(new PomFileReadActor(configuration))
}