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

import java.util.Locale

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.tools.HttpDownloader
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}

class MavenDownloadActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      implicit val system : ActorSystem = context.system

      val downloader = new HttpDownloader

      val jarStream = downloader.downloadFromUri(m.toJarLocation.toString())
      val pomResponse = downloader.downloadFromUriWithHeaders(m.toPomLocation.toString())

      jarStream match {
        case Success(jar) => {
          pomResponse match {
            case Success((pomStream, pomHeaders)) => {
              log.info(s"Downloaded $m")

              // Extract and parse publication date from header
              val datePattern = DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.ENGLISH)
              val pomPublicationDate = pomHeaders.find( _.lowercaseName().equals("last-modified") )
                .map( header => Try(datePattern.parseDateTime(header.value())) ) match {
                case Some(Success(date)) => Some(date)
                case Some(Failure(x)) => x.printStackTrace(); None
                case _ => None
              }

              val pomFile = PomFile(Stream.continually(pomStream.read).takeWhile(_ != -1).map(_.toByte).toArray)

              // Build and initialize metadata from POM
              val metadata = MavenArtifactMetadata.readFromPom(pomPublicationDate.orNull, pomFile).orNull

              sender() ! Success(MavenArtifact(m, JarFile(jar, m.toJarLocation.toURL), pomFile, metadata))
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

