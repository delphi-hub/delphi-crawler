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

import java.security.SecureRandom

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import de.upb.cs.swt.delphi.crawler.tools.ActorStreamIntegrationSignals.{Ack, StreamCompleted, StreamFailure, StreamInitialized}

/**
  * Playground to test things out.
  */
object Playground extends App {

  implicit val system : ActorSystem = ActorSystem("trial")
  implicit val materializer = ActorMaterializer()

  val actorPool = system.actorOf(RoundRobinPool(2).props(PrintActor.props))

  //Source(List("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"))
  Source(1 to 100)
    .runWith(Sink.actorRefWithAck(actorPool, StreamInitialized, Ack, StreamCompleted, StreamFailure))

  val requestedUri = "http://repo1.maven.org/maven2/"

}

class PrintActor extends Actor with ActorLogging {
  private val id = new SecureRandom().nextInt()
  override def receive: Receive = {
    case StreamInitialized =>
      log.info(s"$id - Stream initialized!")
      sender() ! Ack
    case StreamCompleted =>
      log.info(s"$id - Stream completed!")
    case StreamFailure(ex) =>
      log.error(ex, s"$id - Stream failed!")
    case m => {
      log.info(s"$id - $m")
      sender() ! Ack
    }
  }
}
object PrintActor {
  def props = Props(new PrintActor)
}
