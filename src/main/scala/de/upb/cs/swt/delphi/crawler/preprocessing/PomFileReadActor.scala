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

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.util.{Failure, Success, Try}

class PomFileReadActor extends Actor with ActorLogging{

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()

  override def receive: Receive = {
    case artifact@MavenArtifact(identifier, _ ,PomFile(pomStream), _, _) =>

      val pomObject = Try(pomReader.read(pomStream))
      pomStream.close()

      pomObject match {
        case Success(pom) =>

          val metadata = MavenArtifactMetadata(pom.getName, pom.getDescription)
          sender() ! Success(MavenArtifact.withMetadata(artifact, metadata))

        case Failure(ex) =>
          log.error(s"Failed to parse POM file for artifact $identifier",ex )
          sender() ! Failure(ex)
      }

  }
}

object PomFileReadActor {
  def props: Props = Props(new PomFileReadActor)
}
