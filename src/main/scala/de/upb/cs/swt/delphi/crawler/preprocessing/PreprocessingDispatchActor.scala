package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor
import akka.pattern.ask
import akka.routing.{BalancingPool, RoundRobinPool}
import akka.util.Timeout
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream

import scala.concurrent.duration._

class PreprocessingDispatchActor(configuration : Configuration, nextStep : ActorRef) extends Actor with ActorLogging {

  val elasticPool = context.actorOf(RoundRobinPool(configuration.elasticActorPoolSize)
    .props(ElasticActor.props(HttpClient(configuration.elasticsearchClientUri))))
  val callGraphPool = context.actorOf(BalancingPool(configuration.callGraphStreamPoolSize)
    .props(CallGraphStream.props(configuration)))

  override def receive: Receive = {
    case m : MavenIdentifier => {
      
      // Start creation of base record
      elasticPool forward m

      // Create call graphs for each project
      callGraphPool ! m

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