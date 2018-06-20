package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact

class ProcessingDispatchActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenArtifact => {
      // start hermes processing
      //val hermesActor = context.actorOf(HermesActor.props)
      //hermesActor forward m

      // trigger further processing
    }
  }
}
object ProcessingDispatchActor {
  def props = Props(new ProcessingDispatchActor)
}
