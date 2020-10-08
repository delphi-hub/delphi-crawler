package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActorResponse
import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.Ack

class ProcessingFailureStorageActor extends Actor with ActorLogging{
  override def receive: Receive = {
    case response@MavenDownloadActorResponse(identifier, None,true, _, _, errorMessage) =>
      log.info(s"Processing failed pom download for $identifier, message: $errorMessage")
      sender() ! Ack

    case response@MavenDownloadActorResponse(_, Some(_), false, false, false, _) =>
      // This is the "all good" case, no need to do anything
      sender() ! Ack
  }
}

object ProcessingFailureStorageActor {
  def props: Props = Props(new ProcessingFailureStorageActor)
}
