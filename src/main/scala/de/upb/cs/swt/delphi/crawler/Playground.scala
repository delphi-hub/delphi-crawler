package de.upb.cs.swt.delphi.crawler

import akka.actor.{ActorRef, ActorSystem}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.{ElasticClient, HttpClient}
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor

/**
  * Preliminary starter
  */
object Playground extends App {

  val system : ActorSystem = ActorSystem("trial")

  val clientUri = ElasticsearchClientUri("localhost", 9200)
  val elastic : ActorRef = system.actorOf(ElasticActor.props(ElasticClient(clientUri)), "elastic")

  //val maven : ActorRef = system.actorOf(MavenCrawlActor.props(Uri("http://repo1.maven.org/maven2/de/tu-darmstadt/"), elastic), "maven")

  //maven ! StartDiscover
}
