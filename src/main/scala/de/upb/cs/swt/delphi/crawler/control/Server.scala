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
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{complete, get, path, post, _enhanceRouteWithConcatenation}
import akka.stream.Materializer
import de.upb.cs.swt.delphi.crawler.{AppLogging, BuildInfo}

import scala.concurrent.ExecutionContext

class Server(port: Int)
            (implicit system: ActorSystem, mat: Materializer) extends AppLogging {

  implicit val ec : ExecutionContext = system.dispatcher

  val route: Route =
      path("version") {version} ~
      path("stop") {stop}

  private def version= {
    get {
      complete {
        BuildInfo.version
      }
    }
  }

  private def stop = {
    post {
      val timeout = 2000
      new Thread(){
        override def run(){
          Thread.sleep(timeout)
          system.terminate() andThen {case _ => sys.exit(0)}
        }

      }.start()

      complete(s"Shutdown will be executed in $timeout milliseconds.")
    }
  }

  def start(): Unit = {
    Http().bind("0.0.0.0", port).runForeach(_.handleWith(Route.handlerFlow(route)))
    log.info(s"Interaction server started on port $port")
  }
}
