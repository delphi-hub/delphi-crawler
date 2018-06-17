package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class PreprocessingDispatchActor(configuration : Configuration) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      // Start creation of base record
      val elasticActor = context.actorOf(ElasticActor.props(HttpClient(configuration.elasticsearchClientUri)))
      elasticActor forward m

      // Transform maven identifier into maven artifact
      implicit val timeout = Timeout(5.seconds)
      val downloadActor = context.actorOf(MavenDownloadActor.props)
      val mavenArtifact = downloadActor ? m

      // After transformation push to processing dispatch

    }


  }

}

object PreprocessingDispatchActor {
  def props(configuration: Configuration) = Props(new PreprocessingDispatchActor(configuration))
}