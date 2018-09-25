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

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{get, path, complete}
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import de.upb.cs.swt.delphi.crawler.{AppLogging, BuildInfo}
import akka.dispatch.MonitorableThreadFactory
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class Server(port: Int)
            (implicit system: ActorSystem, mat: Materializer) extends AppLogging {

  val route: Route =
    get {
      path("version") {
        version
      }
    };
    post {
      path("stop"){
        stop
      }
    }

  private def version: Route = path("version")
  {
    get {
      complete {
        BuildInfo.version
      }
    }
  }
  private def stop: Route = path("stop")
  {
    post
    {
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        val terminate: Future[Terminated] = system.terminate()
        Await.result(terminate, Duration("10 seconds"))
      }
    }))

    system.registerOnTermination {
      println("ActorSystem terminated")
    }
    complete {"Crawler Shutdown"}
    }
  }

  def start(): Unit = {
    Http().bind("0.0.0.0", port).runForeach(_.handleWith(Route.handlerFlow(route)))
    log.info(s"Interaction server started on port $port")
  }

}
