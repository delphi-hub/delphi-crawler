// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler

import akka.actor.{ActorRef, ActorSystem}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor

/**
  * Preliminary starter
  */
object Playground extends App {

  val system : ActorSystem = ActorSystem("trial")

  val clientUri = ElasticsearchClientUri("localhost", 9200)
  val elastic : ActorRef = system.actorOf(ElasticActor.props(HttpClient(clientUri)), "elastic")

  //val maven : ActorRef = system.actorOf(MavenCrawlActor.props(Uri("http://repo1.maven.org/maven2/de/tu-darmstadt/"), elastic), "maven")

  //maven ! StartDiscover
}
