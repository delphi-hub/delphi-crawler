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

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import org.apache.maven.index.reader.IndexReader
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

trait IndexProcessing {

  def createSource(base : URI)(implicit log : LoggingAdapter) : Source[MavenIdentifier, NotUsed] = {
    log.info("Creating source")

    val ir = Try(new MavenIndexReader(base.toURL))
    ir match {
      case Success(indexReader) => {
        Source.unfoldResource[MavenIdentifier, MavenIndexReader](
          () => indexReader,
          reader => reader.read(),
          reader => reader.close())
      }
      case Failure(e) => {
        log.error(s"Could not reach resource. Terminating crawling for $base.")
        Source.empty
      }
    }

  }

  class MavenIndexReader(base : URL) {
    val log = LoggerFactory.getLogger(this.getClass)

    val ir = new IndexReader(null, new HttpResourceHandler(base.toURI.resolve(".index/")))

    log.info("Index Reader created")
    log.debug(ir.getIndexId)
    log.debug(ir.getPublishedTimestamp.toString)
    log.debug(ir.isIncremental.toString)
    log.debug(ir.getChunkNames.toString)

    lazy val cr = ir.iterator().next().iterator()

    def read() : Option[MavenIdentifier] = {
      cr.hasNext() match {
        case true => {
          val kvp = cr.next()
          val identifier = kvp.get("u").split("|".toCharArray)

          val mavenId = MavenIdentifier(base.toString, identifier(0), identifier(1), identifier(2))
          log.debug(s"Producing $mavenId")
          Some(mavenId)
        }
        case false => None
      }
    }
    def close() = { ir.close() }
  }
}
