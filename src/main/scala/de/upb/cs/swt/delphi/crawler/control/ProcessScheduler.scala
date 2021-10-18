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
import de.upb.cs.swt.delphi.crawler.control.ProcessScheduler.{Enqueue, Finalized, UpdateQueue}

import scala.collection.mutable
import scala.util.Try

/**
  * A scheduler for long-running processes which allows introspection into the queues.
  *
  * @author Ben Hermann
  */
class ProcessScheduler extends Actor with ActorLogging {
  private val parallelism: Int = 4
  private val running = mutable.HashSet.empty[Process[_]]
  private val queue = mutable.Queue.empty[Process[_]]

  override def receive: Receive = {
    case Enqueue(process) =>
      queue.enqueue(process)
      self ! UpdateQueue
    case Finalized(process, result) =>
      running.find(_.equals(process)) match {
        case Some(f) =>
          // TODO: Maybe do some permanent logging here
          running.remove(process)
          self ! UpdateQueue
        case None => log.warning(s"Could not finalized process: $process")
      }
    case UpdateQueue =>
      if (running.size < parallelism && queue.nonEmpty) {
        val nextProcess = queue.dequeue()
        running.add(nextProcess)
        val actor = context.actorOf(ProcessActor.props(nextProcess))
        actor ! ProcessActor.Go
      }
    case x => log.warning(s"Scheduler received unknown message. This should never ever happen. Message was: $x")
  }
}

object ProcessScheduler {
  def props: Props = Props(new ProcessScheduler)

  case class Enqueue(process: Process[_])

  case class Finalized(process: Process[_], result: Try[_])

  case object UpdateQueue

  case object List

  object Status extends Enumeration {
    type Status = Value
    val Queued, Running = Value
  }

}
