package de.upb.cs.swt.delphi.crawler.discovery.maven

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.{ElasticClient, HttpClient}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenCrawlActor.Start
import de.upb.cs.swt.delphi.crawler.storage.ArtifactExistsQuery

import scala.collection.mutable

class MavenCrawlActor(configuration: Configuration, nextStep : ActorRef)
                                                    extends Actor
                                                    with ActorLogging
                                                    with IndexProcessing
                                                    with ArtifactExistsQuery {
  val seen = mutable.HashSet[MavenIdentifier]()

  override def receive: Receive = {
    case Start => {
      log.info("Starting Maven discovery process...")

      implicit val materializer = ActorMaterializer()
      implicit val client = ElasticClient(configuration.elasticsearchClientUri)
      implicit val logger = log

      createSource(configuration.mavenRepoBase)
        .throttle(configuration.throttle.element, configuration.throttle.per, configuration.throttle.maxBurst, configuration.throttle.mode)
        .filter(m => {
          val before = seen.contains(m)
          if (!before) seen.add(m)
          !before
        }) // local seen cache to compensate for lags
        .filter(m => !exists(m))  // ask elastic
        .take(configuration.limit) // limit for now
        .runForeach(m => {
          nextStep ! m
        } ) // TODO: instead as runForeach we might consider using the next step actor as a sink


    }
  }
}

object MavenCrawlActor {
  def props(configuration: Configuration, nextStep : ActorRef) = Props(new MavenCrawlActor(configuration, nextStep))

  case object Start
}
