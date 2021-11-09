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

package de.upb.cs.swt.delphi.crawler.control

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.control.ProcessActor.Go

/**
  * A wrapping actor around a blocking process
  *
  * @param process
  * @author Ben Hermann
  */
class ProcessActor(process: Process[_]) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Go =>
      val result = process.start
      sender() ! ProcessScheduler.Finalized(process, result)
  }
}

object ProcessActor {
  def props(process: Process[_]): Props = Props(new ProcessActor(process))

  case object Go

}

