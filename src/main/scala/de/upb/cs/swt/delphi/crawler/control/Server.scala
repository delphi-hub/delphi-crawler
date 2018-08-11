package de.upb.cs.swt.delphi.crawler.control

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{get, path, complete}
import akka.stream.Materializer
import de.upb.cs.swt.delphi.crawler.{AppLogging, BuildInfo}

class Server(port: Int)
            (implicit system: ActorSystem, mat: Materializer) extends AppLogging {

  val route: Route =
    get {
      path("version") {
        version
      }
    }

  private def version = {
    get {
      complete {
        BuildInfo.version
      }
    }
  }

  def start(): Unit = {
    Http().bind("0.0.0.0", port).runForeach(_.handleWith(Route.handlerFlow(route)))
    log.info(s"Interaction server started on port $port")
  }

}
