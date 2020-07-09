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

import scala.util.Success

class PomFileReadActor extends Actor with ActorLogging{

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()

  override def receive: Receive = {
    case artifact@MavenArtifact(_, _ ,PomFile(pomStream), _, _) => {
      //TODO: Errorhandling
      val pomObject = pomReader.read(pomStream)
      pomStream.close()

      val metadata = MavenArtifactMetadata(pomObject.getName, pomObject.getDescription)

      sender() ! Success(MavenArtifact.withMetadata(artifact, metadata))
    }
  }

}

object PomFileReadActor {
  def props = Props(new PomFileReadActor)
}
