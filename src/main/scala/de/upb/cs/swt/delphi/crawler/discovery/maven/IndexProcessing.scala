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

package de.upb.cs.swt.delphi.crawler.discovery.maven

import java.net.{URI, URL}
import java.util
import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.{RestartSource, Source}
import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import org.apache.maven.index.reader.IndexReader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait IndexProcessing {

  def createSource(base: URI)(implicit log: LoggingAdapter): Source[MavenIdentifier, NotUsed] = {
    log.info("Creating source")

    RestartSource.withBackoff(
      minBackoff = 30.seconds,
      maxBackoff = 90.seconds,
      randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
      maxRestarts = 20 // limits the amount of restarts to 20
    ) { () => {
      val ir = Try(new MavenIndexReader(base.toURL))
      ir match {
        case Success(indexReader) =>
          Source.unfoldResource[MavenIdentifier, MavenIndexReader](
            () => indexReader,
            reader => reader.read(),
            reader => reader.close())
        case Failure(e) =>
          log.error(s"Could not reach resource. Terminating crawling for $base.")
          throw e
      }
    }
    }
  }

}

class MavenIndexReader(base: URL) {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  log.info(s"New maven index reader create for $base")
  val ir = new IndexReader(null, new HttpResourceHandler(base.toURI.resolve(".index/")))

  log.info("Index Reader created")
  log.debug(ir.getIndexId)
  log.debug(ir.getPublishedTimestamp.toString)
  log.debug(ir.isIncremental.toString)
  log.debug(ir.getChunkNames.toString)

  lazy val cr = ir.iterator().next().iterator()

  def read(): Option[MavenIdentifier] = {

    def readInternal(kvp: util.Map[String, String]): Option[MavenIdentifier] = {
      val kvpU = kvp.get("u")
      val identifierAttempt = Try(kvpU.split("|".toCharArray))

      identifierAttempt match {
        case Success(identifier) =>
          val mavenId = MavenIdentifier(Some(base.toString), identifier(0), identifier(1), Some(identifier(2)))
          log.debug(s"Producing $mavenId")

           Some(mavenId)
        case Failure(e) =>
          log.warn(s"While processing index we received the following u-value that we could not split $kvpU. Full kvp is $kvp. Exception was $e.")
          None
      }
    }

    if (cr.hasNext) {
      Iterator.continually(readInternal(cr.next())).takeWhile(result => cr.hasNext).collectFirst[MavenIdentifier]({ case Some(x) => x })
    } else {
      None
    }
  }

  def close():Unit = {
    ir.close()
  }
}


