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

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Source
import akka.pattern.ask
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.http.search.SearchResponse
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActorResponse
import de.upb.cs.swt.delphi.crawler.processing.{HermesActorResponse, PomFileReadActorResponse}
import de.upb.cs.swt.delphi.crawler.storage.{delphi, processingError}
import de.upb.cs.swt.delphi.crawler.tools.NotYetImplementedException

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class MavenErrorRetryProcess(configuration: Configuration, elasticPool: ActorRef)
                            (implicit system: ActorSystem) extends MavenProcess(configuration, elasticPool)(system) {

  private implicit val ec: ExecutionContext = system.dispatcher

  private val identifierSource: Source[MavenProcessingError, NotUsed] = esClient.execute {
    search(delphi).types(processingError)
  }.await match {
    case f: RequestFailure =>
      logger.error(s"Failed to retrieve errors from elastic ${f.error.reason}")
      Source.empty[MavenProcessingError]
    case response: RequestSuccess[SearchResponse] =>
      Source.fromIterator(() => response
        .result
        .hits
        .hits
        .map{ hit =>
          MavenProcessingError.fromElasticSource(hit.sourceAsMap)
        }.iterator
      )
    case r@_ =>
      logger.error(s"Got unknown elastic response while querying errors: $r")
      Source.empty[MavenProcessingError]
  }

  override def start: Try[Long] = {
    identifierSource
        .mapAsync(8) { error =>
          (downloaderPool ? error.identifier).mapTo[MavenDownloadActorResponse].map(r => (error, r))
        }
        .filter { case (error, response) =>
          // Only report failed POM download if it was not original error cause
          if(error.errorType != MavenErrorType.PomDownloadFailed && response.pomDownloadFailed) {
            // New error while retrying the old one => report to storage actor?
            ???
          }

          // Only retain an artifact if POM download succeeded and JAR download did not fail again. Not that a failed JAR
          // download may be okay, if the original error was a failed POM download and the packaging is not set to JAR.
          !response.pomDownloadFailed && (error.errorType != MavenErrorType.JarDownloadFailed || !response.jarDownloadFailed)
        }
        .mapAsync(8){ case (error, response) =>
          (pomReaderPool ? response).mapTo[PomFileReadActorResponse].map(r => (error, r))
        }
        .filter {case (error, response) =>

          // Only retain artifact if jar download did not fail, otherwise Hermes cannot be invoked.
          !response.jarDownloadFailed
        }
        .mapAsync(configuration.hermesActorPoolSize){ case (error, response) =>
          (hermesPool ? response.artifact).mapTo[HermesActorResponse].map(r => (error, r))
        }
        //TODO: Report new errors, store results after each step!




    Success(0L)
  }

  override def stop: Try[Long] = {
    Failure(new NotYetImplementedException)
  }



}
