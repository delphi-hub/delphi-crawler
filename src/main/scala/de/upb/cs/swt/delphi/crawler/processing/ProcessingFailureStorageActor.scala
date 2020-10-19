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

package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.{MavenIdentifier, MavenProcessingError}
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenDownloadActorResponse}
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}

import scala.util.Failure

class ProcessingFailureStorageActor(elasticPool: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {

    case StreamInitialized =>
      log.info(s"Stream initialized!")
      sender() ! Ack

    case StreamCompleted =>
      log.info(s"Stream completed!")

    case StreamFailure(ex) =>
      log.error(ex, s"Stream failed!")

    case MavenDownloadActorResponse(identifier, None,true, _, _, errorMessage) =>
      // POM Download failed, this is always an error
      log.info(s"Processing failed pom download for $identifier, message: $errorMessage")
      storeError(MavenProcessingError.createPomDownloadError(identifier, errorMessage))
      sender() ! Ack

    case MavenDownloadActorResponse(identifier, _, false, false, true, errorMessage) =>
      // Publish date parsing failed, does not hinder further processing
      log.info(s"Processing failed publish date extraction for $identifier, message: $errorMessage")
      sender() ! Ack

    case PomFileReadActorResponse(MavenArtifact(identifier,_,_,_,Some(meta)),true, _, errorMessage)
      if meta.packaging.equalsIgnoreCase("jar") =>
      // JAR Download failed although POM file said jar should exist, this is an error
      log.info(s"Processing failed jar download for $identifier")
      storeError(MavenProcessingError.createJarDownloadError(identifier, errorMessage))
      sender() ! Ack

    case PomFileReadActorResponse(MavenArtifact(identifier,_,_,_,_), _, true, errorMessage) =>
      // POM parsing failed
      log.info(s"Processing failed pom processing for $identifier, message: $errorMessage")
      storeError(MavenProcessingError.createPomParsingError(identifier, errorMessage))
      sender() ! Ack

    case HermesActorResponse(identifier: MavenIdentifier, Failure(ex)) =>
      // Hermes processing failed
      log.info(s"Processing failed Hermes analysis for $identifier, message: ${ex.getMessage}")
      storeError(MavenProcessingError.createHermesProcessingError(identifier, ex.getMessage))
      sender() ! Ack

    case response
      if response.isInstanceOf[MavenDownloadActorResponse] || response.isInstanceOf[PomFileReadActorResponse] ||
        response.isInstanceOf[HermesActorResponse] =>
      sender() ! Ack

    case msg@_ =>
      log.error(s"Invalid message format: $msg")
      sender() ! StreamFailure
  }

  private def storeError(error: MavenProcessingError): Unit = elasticPool forward error
}

object ProcessingFailureStorageActor {
  def props(elasticPool: ActorRef): Props = Props(new ProcessingFailureStorageActor(elasticPool))
}
