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

package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.model.MavenArtifact

class ProcessingDispatchActor(hermesActor : ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenArtifact => {
      // start hermes processing
      hermesActor forward m

      // trigger further processing
    }
  }
}
object ProcessingDispatchActor {
  def props(hermesActor : ActorRef) = Props(new ProcessingDispatchActor(hermesActor))
}
