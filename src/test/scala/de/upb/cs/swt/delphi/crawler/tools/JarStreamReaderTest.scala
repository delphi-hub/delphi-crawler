package de.upb.cs.swt.delphi.crawler.tools

import java.io.BufferedInputStream
import java.net.{HttpURLConnection, URL}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.io.File
import scala.util.Random

class JarStreamReaderTest extends FlatSpec with Matchers {
  "A JarStreamReader" should "be able to read from a file input stream" in {
    val fis = File("src/test/resources/sootconfig-1.1.jar").inputStream()
    val jsr = new JarStreamReader(new BufferedInputStream(fis))
    val entries = jsr.readFully()

    entries.size should be (25)
  }

  it should "be able to read from a HTTP resource" in {

    val route: Route =
      get {
        path("sootconfig-1.1.jar") {
          getFromFile("src/test/resources/sootconfig-1.1.jar")
        }
      }
    implicit val system : ActorSystem = ActorSystem("delphi-crawler")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec : ExecutionContext = system.dispatcher

    val port = Random.nextInt(10000) + 10000

    val f = Http().bindAndHandle(Route.handlerFlow(route), "0.0.0.0", port )

    Await.ready(Future { Thread.sleep(100); true}, 150.milliseconds)

    val target = new URL(s"http://127.0.0.1:$port/sootconfig-1.1.jar")
    val conn = target.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    conn.setRequestProperty("User-Agent", s"Delphi Maven-Indexer-Reader/1.0.0-SNAPSHOT")

    val bufferedInputStream = new BufferedInputStream(conn.getInputStream)
    val jsr = new JarStreamReader(bufferedInputStream)
    val entries = jsr.readFully()

    entries.size should be (25)
  }

}
