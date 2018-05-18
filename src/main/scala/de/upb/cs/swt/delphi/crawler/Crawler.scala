package de.upb.cs.swt.delphi.crawler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import de.upb.cs.swt.delphi.crawler.control.Server

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * The starter for Delphi Crawler
  */
object Crawler extends App with AppLogging {
  private val configuration = new Configuration()

  implicit val system : ActorSystem = ActorSystem("delphi-crawler")
  implicit val materializer = ActorMaterializer()

  sys.addShutdownHook(() => {
    log.warning("Received shutdown signal.")
    val future = system.terminate()
    Await.result(future, 120.seconds)
  })


  Startup.showStartupInfo
  Startup.preflightCheck(configuration) match {
    case Success(c) => new Server(configuration.controlServerPort).start()
    case Failure(e) => {
      system.terminate()
      sys.exit(1)
    }
  }

}
