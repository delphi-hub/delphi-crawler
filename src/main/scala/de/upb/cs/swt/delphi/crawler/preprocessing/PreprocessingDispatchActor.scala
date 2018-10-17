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
import akka.pattern.ask
import akka.routing.{BalancingPool, RoundRobinPool, SmallestMailboxPool}
import akka.util.Timeout
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream
import de.upb.cs.swt.delphi.crawler.storage.ElasticActor

import scala.concurrent.duration._
import scala.util.Success

class PreprocessingDispatchActor(configuration : Configuration, nextStep : ActorRef, elasticPool : ActorRef) extends Actor with ActorLogging {

  val callGraphPool = context.actorOf(BalancingPool(configuration.callGraphStreamPoolSize)
    .props(CallGraphStream.props(configuration)))

  val downloadActorPool = context.actorOf(SmallestMailboxPool(8).props(MavenDownloadActor.props))

  override def receive: Receive = {
    case m : MavenIdentifier => {

      implicit val ec = context.dispatcher

      // Start creation of base record
      elasticPool forward m

      // Transform maven identifier into maven artifact
      implicit val timeout = Timeout(1 minutes)
      val f = downloadActorPool ? m

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