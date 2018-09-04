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
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

class MavenDownloadActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      val downloader = new MavenDownloader(m)
      val jar = downloader.downloadJar()
      val pom = downloader.downloadPom()
      sender() ! MavenArtifact(m, jar, pom)
    }
  }
}
object MavenDownloadActor {
  def props = Props(new MavenDownloadActor)
}

