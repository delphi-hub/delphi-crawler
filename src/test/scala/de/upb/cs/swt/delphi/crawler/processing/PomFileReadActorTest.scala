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

import akka.actor.ActorSystem
import akka.testkit.TestKit
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.model.{ArtifactDependency, MavenArtifact, MavenArtifactMetadata}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActor
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.ask
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticApi.RichFuture

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class PomFileReadActorTest extends TestKit(ActorSystem("PomFileReadActor")) with AnyWordSpecLike with should.Matchers {
  private val theConfig = new Configuration()

  private val oldPomIdent: MavenIdentifier =
    new MavenIdentifier(Some(theConfig.mavenRepoBase.toString), "xfire", "xfire-loom", Some("1.0-20051009.195948"))

  private val newPomIdent: MavenIdentifier =
    new MavenIdentifier(Some(theConfig.mavenRepoBase.toString), "com.amazonaws", "aws-java-sdk", Some("1.12.91"))

  "The PomFileReadActor" must {
    "read old POM files without interpolation" in {
      val metadata = extractMetadata(oldPomIdent)

      assert(metadata.issueManagement.isDefined)
      assert(metadata.issueManagement.get.url.equalsIgnoreCase("http://jira.codehaus.org/secure/BrowseProject.jspa?id=10750"))

      assert(metadata.parent.isEmpty)
      assert(metadata.developers.size == 6)
      assert(metadata.dependencies.size == 20)
      assert(metadata
        .dependencies.exists {
          case ArtifactDependency(MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "wsdl4j", "wsdl4j", Some("1.4")), Some("compile")) =>
            true
          case _ => false
        }
      )

      // Default packaging in Maven
      assert(metadata.packaging.equalsIgnoreCase("jar"))

      assert(metadata.name.equals("Loom XFire Module"))
    }

    "read new POM files with interpolation" in {
      val metadata = extractMetadata(newPomIdent)

      assert(metadata.issueManagement.isEmpty)

      assert(metadata.name.equals("AWS SDK For Java"))
      assert(metadata.description.length > 100)

      assert(metadata.parent.isDefined)
      assert(metadata.parent.get.artifactId.equals("aws-java-sdk-pom") && metadata.parent.get.version.get.equals("1.12.91"))

      assert(metadata.dependencies.exists(d => d.identifier.artifactId.equals("aws-java-sdk-models")))
      metadata
        .dependencies
        .filter(d => d.identifier.artifactId.equals("aws-java-sdk-models"))
        .foreach(d => assert(d.identifier.version.isDefined && d.identifier.version.get.equals("1.12.91")))
    }
  }

  private def extractMetadata(ident: MavenIdentifier): MavenArtifactMetadata = {
    implicit val askTimeout: Timeout = Timeout(30 seconds)

    val artifact = downloadArtifact(ident)
    val theActor = system.actorOf(PomFileReadActor.props(theConfig))

    (theActor ? artifact).await.asInstanceOf[MavenArtifact] match {
      case MavenArtifact(_,_,_,_, Some(metadata)) =>
        metadata
      case MavenArtifact(_,_,_,_, None) =>
        fail(s"PomFileReadActor failed to extract metadata for ${ident.toString}")
    }
  }

  private def downloadArtifact(ident: MavenIdentifier): MavenArtifact = {
    implicit val downloadTimeout: Timeout = Timeout(10 seconds)
    val downloader = system.actorOf(MavenDownloadActor.props)

    (downloader ? ident).await.asInstanceOf[Try[MavenArtifact]] match {
      case Success(artifact) =>
        artifact
      case Failure(ex) =>
        fail(s"Failed to download artifact for ${ident.toString}", ex)
    }
  }
}
