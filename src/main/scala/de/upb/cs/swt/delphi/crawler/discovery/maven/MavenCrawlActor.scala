package de.upb.cs.swt.delphi.crawler.discovery.maven

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenCrawlActor.Start
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery

import scala.collection.mutable
import scala.concurrent.duration._

class MavenCrawlActor(storageActor : ActorRef,
                     configuration: Configuration)  extends Actor
                                                    with ActorLogging
                                                    with IndexProcessing
                                                    with ArtifactExistsQuery {
  val seen = mutable.HashSet[MavenIdentifier]()

  override def receive: Receive = {
    case Start => {
      log.info("Starting Maven discovery process...")
      implicit val materializer = ActorMaterializer()
      implicit val httpClient = HttpClient(configuration.elasticsearchClientUri)
      implicit val logger = log
      createSource
        .throttle(10, 10 millis, 10, ThrottleMode.shaping)
        .filter(!seen.contains(_)) // local seen cache to compensate for lags
        .map{m => seen.add(m); m}
        .filter(m => !exists(m))
        .take(50) // limit for now
        .runForeach(m => storageActor ! m) // TODO maybe formulate initial load as stream components and not as actors?
        //.to(Sink.actorRef(storageActor, Status.Success))  // For some reason this does not pull data
    }
  }
}

object MavenCrawlActor {
  def props(storageActor : ActorRef, configuration: Configuration) = Props(new MavenCrawlActor(storageActor, configuration))

  case object Start
}
