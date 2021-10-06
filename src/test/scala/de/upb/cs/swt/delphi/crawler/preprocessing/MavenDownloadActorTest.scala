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

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import de.upb.cs.swt.delphi.crawler.preprocessing.Common._

import scala.concurrent.Await
import scala.util.{Success, Try}

/**
  * @author Hariharan.
  * @author Ben Hermann
  */
class MavenDownloadActorTest extends TestKit(ActorSystem("DownloadActor"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  "The maven download actor" must {
    "create a maven artifact with a jar and pom file" in {
      val mavenIdentifier = new MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "junit", "junit", Some("4.12"))
      val downloadActor = system.actorOf(MavenDownloadActor.props)

      implicit val timeout = Timeout(10 seconds)
      implicit val ec = system.dispatcher

      val f = downloadActor ? mavenIdentifier

      val msg = Await.result(f, 10 seconds)

      assert(msg.isInstanceOf[Success[MavenArtifact]])
      val artifact = msg.asInstanceOf[Success[MavenArtifact]].get
      checkJar(artifact.jarFile.is)
      checkPom(artifact.pomFile.is)


    }
  }
}
