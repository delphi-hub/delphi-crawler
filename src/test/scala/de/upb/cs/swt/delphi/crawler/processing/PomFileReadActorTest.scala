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
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenDownloadActor}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success

class PomFileReadActorTest extends TestKit(ActorSystem("DownloadActor"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The POM file reader actor " must {
    "create a maven artifact with valid metadata" in {
      val mavenIdentifier = new MavenIdentifier("https://repo1.maven.org/maven2/", "junit", "junit", "4.12")
      val downloadActor = system.actorOf(MavenDownloadActor.props)
      val readerActor = system.actorOf(PomFileReadActor.props(new Configuration()))

      implicit val timeout: Timeout = Timeout(10 seconds)
      implicit val ec: ExecutionContext = system.dispatcher

      val f = downloadActor ? mavenIdentifier

      val msg = Await.result(f, 10 seconds)

      assert(msg.isInstanceOf[Success[MavenArtifact]])
      val artifact = msg.asInstanceOf[Success[MavenArtifact]].get

      assert(artifact.metadata.isEmpty)
      assert(artifact.publicationDate.isDefined && artifact.publicationDate.get != null)

      val result = Await.result(readerActor ? artifact, 10 seconds)
      assert(result.isInstanceOf[Success[MavenArtifact]])
      val annotatedArtifact = result.asInstanceOf[Success[MavenArtifact]].get

      assert(annotatedArtifact.metadata.isDefined)
      val metadata = annotatedArtifact.metadata.get

      assert(metadata.name != null && metadata.name.equals("JUnit"))
      assert(metadata.description != null && metadata.description.startsWith("JUnit is a unit testing framework for Java,"))

      assert(metadata.issueManagement.isDefined)
      assertResult("https://github.com/junit-team/junit/issues")(metadata.issueManagement.get.url)
      assertResult("github")(metadata.issueManagement.get.system)

      assertResult(4)(metadata.developers.size)

      assertResult(1)(metadata.licenses.size)
      assertResult("Eclipse Public License 1.0")(metadata.licenses.head.name)
    }

    "process dependencies as expected" in {
      val mavenIdentifier = new MavenIdentifier("https://repo1.maven.org/maven2/", "org.apache.bookkeeper", "bookkeeper-server", "4.9.2")
      val downloadActor = system.actorOf(MavenDownloadActor.props)
      val readerActor = system.actorOf(PomFileReadActor.props(new Configuration()))

      implicit val timeout: Timeout = Timeout(10 seconds)
      implicit val ec: ExecutionContext = system.dispatcher

      val f = downloadActor ? mavenIdentifier

      val msg = Await.result(f, 10 seconds)

      assert(msg.isInstanceOf[Success[MavenArtifact]])
      val artifact = msg.asInstanceOf[Success[MavenArtifact]].get

      val result = Await.result(readerActor ? artifact, 10 seconds)
      assert(result.isInstanceOf[Success[MavenArtifact]])
      val annotatedArtifact = result.asInstanceOf[Success[MavenArtifact]].get

      val dependencies = annotatedArtifact.metadata.get.dependencies

      assertResult(8)(dependencies.size)
      assertResult(8)(dependencies.count(_.version == "4.9.2"))
      assert(dependencies.contains(MavenIdentifier("https://repo1.maven.org/maven2/","org.apache.bookkeeper", "circe-checksum", "4.9.2")))
    }
  }

}
