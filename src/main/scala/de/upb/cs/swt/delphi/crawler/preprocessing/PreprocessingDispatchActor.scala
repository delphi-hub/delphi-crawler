package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor

import scala.concurrent.duration._
import scala.util.{Success, Try}

class PreprocessingDispatchActor(configuration : Configuration, nextStep : ActorRef, elasticActor : ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {

      implicit val ec = context.dispatcher

      // Start creation of base record
      elasticActor forward m

      // Transform maven identifier into maven artifact
      implicit val timeout = Timeout(5.seconds)
      val downloadActor = context.actorOf(MavenDownloadActor.props)
      val f = downloadActor ? m

      // After transformation push to processing dispatch
      f.onComplete { result => result match {
          case Success(mavenArtifact) => nextStep ! mavenArtifact
          case x => log.error(s"Stuff happened: $x")
        }
      }

    }


  }

}

object PreprocessingDispatchActor {
  def props(configuration: Configuration, nextStep : ActorRef, elasticActor: ActorRef) = Props(new PreprocessingDispatchActor(configuration, nextStep, elasticActor))
}