package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenArtifact, MavenDownloadActorResponse}
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}

import scala.util.{Failure, Success}

class ProcessingFailureStorageActor extends Actor with ActorLogging{
  override def receive: Receive = {

    case StreamInitialized =>
      log.info(s"Stream initialized!")
      sender() ! Ack
    case StreamCompleted =>
      log.info(s"Stream completed!")
    case StreamFailure(ex) =>
      log.error(ex, s"Stream failed!")

    case response@MavenDownloadActorResponse(identifier, None,true, _, _, errorMessage) =>
      // POM Download failed, this is always an error
      log.info(s"Processing failed pom download for $identifier, message: $errorMessage")
      sender() ! Ack

    case response@MavenDownloadActorResponse(identifier, _, false, false, true, errorMessage) =>
      // Publish date parsing failed, does not hinder further processing
      log.info(s"Processing failed publish date extraction for $identifier, message: $errorMessage")
      sender() ! Ack

    case response@PomFileReadActorResponse(MavenArtifact(identifier,_,_,_,Some(meta)),true, _, _)
      if meta.packaging.equalsIgnoreCase("jar") =>
      // JAR Download failed although POM file said jar should exist, this is an error
      log.info(s"Processing failed jar download for $identifier")
      sender() ! Ack

    case response@PomFileReadActorResponse(MavenArtifact(identifier,_,_,_,_), _, true, errorMessage) =>
      // POM parsing failed
      log.info(s"Processing failed pom processing for $identifier, message: $errorMessage")
      sender() ! Ack

    case (identifier: MavenIdentifier, Failure(ex)) =>
      // Hermes processing failed
      log.info(s"Processing failed Hermes analysis for $identifier, message: ${ex.getMessage}")
      sender() ! Ack

    case response@MavenDownloadActorResponse(_, Some(_), false, false, false, _) =>
      // This is the "all good" case, no need to do anything
      log.info("Got an all good response with no errors from MavenDownloadActor!")
      sender() ! Ack

    case response@PomFileReadActorResponse(_, false, false, _) =>
      // This is the "all good" case, no need to do anything
      log.info("Got an all good response with no errors from PomFileReadActor!")
      sender() ! Ack

    case (_, Success(_)) =>
      // This is the "all good" case, no need to do anything
      log.info(s"Got an all good response with no errors from HermesActor!")
      sender() ! Ack

    case response@_ =>
      log.info(s"Got unexpected response format: $response")
      sender() ! Ack
  }
}

object ProcessingFailureStorageActor {
  def props: Props = Props(new ProcessingFailureStorageActor)
}
