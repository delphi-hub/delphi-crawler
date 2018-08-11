package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact

class ProcessingDispatchActor(hermesActor : ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenArtifact => {
      // start hermes processing
      hermesActor forward m

      // trigger further processing
    }
  }
}
object ProcessingDispatchActor {
  def props(hermesActor : ActorRef) = Props(new ProcessingDispatchActor(hermesActor))
}
