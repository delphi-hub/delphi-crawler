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
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}

class MavenDownloadActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case m : MavenIdentifier =>
      implicit val system : ActorSystem = context.system

      val downloader = new HttpDownloader

      val pomResponse = downloader.downloadFromUriWithHeaders(m.toPomLocation.toString)

      pomResponse match {
        case Success((pomStream, pomHeaders)) =>
          log.info(s"Downloaded $m")

          // Extract and parse publication date from header
          val datePattern = DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.ENGLISH)
          val pomPublicationDate = pomHeaders.find( _.lowercaseName().equals("last-modified") )
            .map( header => Try(datePattern.parseDateTime(header.value())) ) match {
            case Some(Success(date)) => Some(date)
            case Some(Failure(x)) =>
              log.warning(s"Failed to extract publication date for $m: ${x.getMessage}")
              None
            case _ => None
          }

          downloader.downloadFromUri(m.toJarLocation.toString) match {
            case Success(jar) =>
              sender() ! MavenDownloadActorResponse(
                m,
                Some(MavenArtifact(m, Some(JarFile(jar, m.toJarLocation.toURL)), PomFile(pomStream), pomPublicationDate, None)),
              dateParsingFailed = pomPublicationDate.isEmpty)
            case Failure(ex) =>
              log.warning(s"Failed to download jar file for $m")
              sender() ! MavenDownloadActorResponse(
                m,
                Some(MavenArtifact(m, None, PomFile(pomStream), pomPublicationDate, None)),
                jarDownloadFailed = true,
                dateParsingFailed = pomPublicationDate.isEmpty,
                errorMessage = ex.getMessage
              )
          }

        case Failure(ex) =>
          log.error(s"Failed to download pom file for $m with message: ${ex.getMessage}")
          sender() ! MavenDownloadActorResponse(m, None, pomDownloadFailed = true, errorMessage = ex.getMessage)
      }

  }
}

case class MavenDownloadActorResponse(identifier: MavenIdentifier,
                                      artifact: Option[MavenArtifact],
                                      pomDownloadFailed: Boolean = false,
                                      jarDownloadFailed: Boolean = false,
                                      dateParsingFailed: Boolean = false,
                                      errorMessage: String = "")

object MavenDownloadActor {
  def props: Props = Props(new MavenDownloadActor)
}

