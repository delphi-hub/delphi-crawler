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

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.tools.HttpDownloader

import scala.util.{Failure, Success}

class MavenDownloadActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      implicit val system : ActorSystem = context.system

      val downloader = new HttpDownloader

      val jarStream = downloader.downloadFromUri(m.toJarLocation.toString())
      val pomStream = downloader.downloadFromUri(m.toPomLocation.toString())

      jarStream match {
        case Success(jar) => {
          pomStream match {
            case Success(pom) => {
              log.info(s"Downloaded $m")
              sender() ! Success(MavenArtifact(m, JarFile(jar, m.toJarLocation.toURL), PomFile(pom)))
            }
            case Failure(e) => {
              // TODO: push error to actor
              log.warning(s"Failed pom download for $m")
              sender() ! Failure(e)
            }
          }
        }
        case Failure(e) => {
          // TODO: push error to actor
          log.warning(s"Failed jar download for $m")
          sender() ! Failure(e)
        }
      }


    }
  }
}
object MavenDownloadActor {
  def props = Props(new MavenDownloadActor)
}

