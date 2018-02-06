package de.upb.cs.swt.delphi.crawler.maven

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.elastic.ElasticActor.Push
import de.upb.cs.swt.delphi.crawler.maven.MavenCrawlActor.{Discover, ProcessNew}


/**
  * Created by benhermann on 05.02.18.
  */
class MavenCrawlActor(baseURL : URL, elasticActor : ActorRef) extends Actor with ActorLogging {
  override def receive = {
    case Discover => {
      log.info("Discovery run for baseURL {}", baseURL)
      self ! ProcessNew(new MavenIdentifier("g", "a", "v"))
    }
    case ProcessNew(m : MavenIdentifier) => {
      elasticActor ! Push(m)

      // retrieve binary
      // push to hermes
    }
  }
}

object MavenCrawlActor {
  def props(baseURL : URL, elasticActor : ActorRef) : Props = Props(new MavenCrawlActor(baseURL, elasticActor))

  case object Discover
  case class ProcessNew(identifier : MavenIdentifier)

}
