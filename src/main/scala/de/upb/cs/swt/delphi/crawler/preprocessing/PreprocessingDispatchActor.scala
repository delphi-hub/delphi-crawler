package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor

class PreprocessingDispatchActor(configuration : Configuration, nextStep : ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      
      // Start creation of base record
      val elasticActor = context.actorOf(ElasticActor.props(ElasticClient(configuration.elasticsearchClientUri)))
      elasticActor forward m

      // Transform maven identifier into maven artifact
      /*implicit val timeout = Timeout(5.seconds)
      val downloadActor = context.actorOf(MavenDownloadActor.props)
      val mavenArtifact = downloadActor ? m
      */
      // After transformation push to processing dispatch
      //nextStep ! mavenArtifact
    }


  }

}

object PreprocessingDispatchActor {
  def props(configuration: Configuration, nextStep : ActorRef) = Props(new PreprocessingDispatchActor(configuration, nextStep))
}