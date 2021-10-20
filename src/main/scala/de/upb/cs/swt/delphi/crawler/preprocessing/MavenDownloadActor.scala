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
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.model.{JarFile, MavenArtifact, PomFile, ProcessingPhaseFailedException}
import de.upb.cs.swt.delphi.crawler.tools.{HttpDownloader, HttpException}
import org.joda.time.format.DateTimeFormat

import java.util.Locale
import scala.util.{Failure, Success, Try}

class MavenDownloadActor extends Actor with ActorLogging {

  private val datePattern =
    DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss zzz").withLocale(Locale.ENGLISH)


  override def receive: Receive = {

    case mavenIdent: MavenIdentifier =>
      implicit val system : ActorSystem = context.system

      val downloader = new HttpDownloader

      downloader.downloadFromUriWithHeaders(mavenIdent.toPomLocation.toString) match {

        case Success((pomStream, pomHeaders)) =>

          // Extract and parse publication date from header
          val pomPublicationDate = pomHeaders
            .find( _.lowercaseName().equals("last-modified") )
            .map( header => Try(datePattern.parseDateTime(header.value())) ) match {
            case Some(Success(date)) => Some(date)
            case _ => None
          }

          downloader.downloadFromUri(mavenIdent.toJarLocation.toString) match {
            case Success(jarStream) =>
              sender() ! Success(MavenArtifact(mavenIdent, PomFile(pomStream),
                Some(JarFile(jarStream, mavenIdent.toJarLocation.toURL)), pomPublicationDate, None))
            case Failure(ex@HttpException(code)) if code.intValue() == 404 =>
              log.warning(s"No JAR file could be located for ${mavenIdent.toUniqueString}")
              sender() ! Success(MavenArtifact(mavenIdent, PomFile(pomStream), None, pomPublicationDate, None))
            case Failure(ex) =>
              log.error(ex, s"Failed to download JAR file for ${mavenIdent.toUniqueString}")
              sender() ! Failure(ProcessingPhaseFailedException(mavenIdent, ex))
          }
        case Failure(ex@HttpException(code)) if code.intValue() == 404 =>
          log.error(s"Failed to download POM file for ${mavenIdent.toUniqueString}")
          sender() ! Failure(ProcessingPhaseFailedException(mavenIdent, ex))
        case Failure(ex) =>
          log.error(ex, s"Failed to download POM file for ${mavenIdent.toUniqueString}")
          sender() ! Failure(ProcessingPhaseFailedException(mavenIdent, ex))
      }
    }
}
object MavenDownloadActor {
  def props: Props = Props(new MavenDownloadActor)
}

