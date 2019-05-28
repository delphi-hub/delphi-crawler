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

import akka.actor.ActorSystem
import akka.routing.{RoundRobinPool, SmallestMailboxPool}
import akka.stream.ActorMaterializer
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.control.{ProcessScheduler, Server}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenDiscoveryProcess
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceRegistry
import de.upb.cs.swt.delphi.crawler.preprocessing.PreprocessingDispatchActor
import de.upb.cs.swt.delphi.crawler.processing.{HermesActor, HermesAnalyzer, ProcessingDispatchActor}
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor
import de.upb.cs.swt.delphi.crawler.tools.OPALLogAdapter
import org.opalj.log.{GlobalLogContext, OPALLogger}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * The starter for Delphi Crawler
  */
object Crawler extends App with AppLogging {
  private val configuration = new Configuration()

  implicit val system: ActorSystem = ActorSystem("delphi-crawler")
  implicit val materializer = ActorMaterializer()

  OPALLogger.updateLogger(GlobalLogContext, OPALLogAdapter)
  HermesAnalyzer.setConfig()

  sys.addShutdownHook({
    log.warning("Received shutdown signal.")
    InstanceRegistry.handleInstanceStop(configuration)
    val future = system.terminate()
    Await.result(future, 120.seconds)
  })


  Startup.showStartupInfo
  Startup.preflightCheck(configuration) match {
    case Success(c) =>
    case Failure(e) => {
      InstanceRegistry.handleInstanceFailure(configuration)
      system.terminate()
      sys.exit(1)
    }
  }


  log.info("Preflight checks completed. Entering flight mode...")

  new Server(configuration.controlServerPort).start()

  val elasticPool = system.actorOf(RoundRobinPool(configuration.elasticActorPoolSize)
    .props(ElasticActor.props(ElasticClient(configuration.elasticsearchClientUri))))

  /*
  val hermesPool = system.actorOf(SmallestMailboxPool(configuration.hermesActorPoolSize).props(HermesActor.props()))
  val processingDispatchActor = system.actorOf(ProcessingDispatchActor.props(hermesPool))

  val preprocessingDispatchActor = system.actorOf(PreprocessingDispatchActor.props(configuration, processingDispatchActor, elasticPool))
*/

  val processScheduler = system.actorOf(ProcessScheduler.props)
  processScheduler ! ProcessScheduler.Enqueue(new MavenDiscoveryProcess(configuration, elasticPool))


}
