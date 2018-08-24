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

package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.sksamuel.elastic4s.http.HttpClient
import akka.util.Timeout
import akka.pattern.ask
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor
import akka.routing.{BalancingPool, RoundRobinPool}
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream

import scala.concurrent.duration._
import scala.util.{Success, Try}

class PreprocessingDispatchActor(configuration : Configuration, nextStep : ActorRef, elasticActor : ActorRef) extends Actor with ActorLogging {

  val elasticPool = context.actorOf(RoundRobinPool(configuration.elasticActorPoolSize)
    .props(ElasticActor.props(HttpClient(configuration.elasticsearchClientUri))))
  val callGraphPool = context.actorOf(BalancingPool(configuration.callGraphStreamPoolSize)
    .props(CallGraphStream.props(configuration)))

  override def receive: Receive = {
    case m : MavenIdentifier => {

      implicit val ec = context.dispatcher

      // Start creation of base record
      elasticPool forward m

      // Create call graphs for each project
      callGraphPool ! m

      // Transform maven identifier into maven artifact
      implicit val timeout = Timeout(5.seconds)
      val downloadActor = context.actorOf(MavenDownloadActor.props)
      val f = downloadActor ? m

      // After transformation push to processing dispatch
      f.onComplete { result => result match {
          case Success(mavenArtifact) => nextStep ! mavenArtifact
          case x => log.error(s"Stuff happened: $x")
        }
      }

    }


  }

}

object PreprocessingDispatchActor {
  def props(configuration: Configuration, nextStep : ActorRef, elasticActor: ActorRef): Props = Props(new PreprocessingDispatchActor(configuration, nextStep, elasticActor))
}