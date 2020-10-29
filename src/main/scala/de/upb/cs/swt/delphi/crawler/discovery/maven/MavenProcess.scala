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

package de.upb.cs.swt.delphi.crawler.discovery.maven

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.routing.SmallestMailboxPool
import akka.util.Timeout
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloadActor
import de.upb.cs.swt.delphi.crawler.processing._
import scala.concurrent.duration._


/**
  * Abstract superclass for processes that deal with Maven artifacts.
  *
  * @author Ben Hermann, Johannes Düsing
  * @param configuration The configuration to be used
  * @param elasticPool   A pool of elasticsearch actors
  * @param system        The currently used actor system (for logging)
  *
  */
abstract class MavenProcess(configuration: Configuration, elasticPool: ActorRef)(implicit system: ActorSystem)
  extends de.upb.cs.swt.delphi.crawler.control.Process[Long] with AppLogging {

  protected implicit val esClient: ElasticClient = ElasticClient(configuration.elasticsearchClientUri)

  protected implicit val processingTimeout: Timeout = Timeout(5.minutes)

  protected implicit val logger: LoggingAdapter = log

  protected val downloaderPool: ActorRef =
    system.actorOf(SmallestMailboxPool(8).props(MavenDownloadActor.props))
  protected val pomReaderPool: ActorRef =
    system.actorOf(SmallestMailboxPool(8).props(PomFileReadActor.props(configuration)))
  protected val errorHandlerPool: ActorRef =
    system.actorOf(SmallestMailboxPool(8).props(ProcessingFailureStorageActor.props(elasticPool)))
  protected val hermesPool: ActorRef =
    system.actorOf(SmallestMailboxPool(configuration.hermesActorPoolSize).props(HermesActor.props()))

  override def phase: Phase = Phase.Discovery
}



