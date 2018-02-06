package de.upb.cs.swt.delphi.crawler

import java.net.URL

import akka.actor.{ActorRef, ActorSystem}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.elastic.ElasticActor
import de.upb.cs.swt.delphi.crawler.maven.MavenCrawlActor
import de.upb.cs.swt.delphi.crawler.maven.MavenCrawlActor.Discover

/**
  * Created by benhermann on 06.02.18.
  */
object Playground extends App {

  val system : ActorSystem = ActorSystem("trial")

  val client = HttpClient(ElasticsearchClientUri("localhost", 9200))
  val elastic : ActorRef = system.actorOf(ElasticActor.props(client), "elastic")

  val maven : ActorRef = system.actorOf(MavenCrawlActor.props(new URL("http://search.maven.org"), elastic), "maven")

  maven ! Discover
}
