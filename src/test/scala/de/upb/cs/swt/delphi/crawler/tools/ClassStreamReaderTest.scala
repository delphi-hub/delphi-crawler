package de.upb.cs.swt.delphi.crawler.tools

import java.io.BufferedInputStream
import java.net.{HttpURLConnection, URL}
import java.security.SecureRandom
import java.util.jar.JarInputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{get, getFromFile, path}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.opalj.br.analyses.Project
import org.opalj.log.{GlobalLogContext, OPALLogger}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.io.File
import scala.util.Random

/**
  * @author Ben Hermann
  */
class ClassStreamReaderTest extends FlatSpec with Matchers with BeforeAndAfter {

  before {
    OPALLogger.updateLogger(GlobalLogContext, OPALLogAdapter)
  }

  "A ClassStreamReader" should "be able to produce a useful OPAL Project instance" in {
    val file = File("src/test/resources/sootconfig-1.1.jar")
    val fis = file.inputStream()
    val jis = new JarInputStream(new BufferedInputStream(fis))

    val p = new ClassStreamReader {}.createProject(file.toURL, jis)

    p shouldBe a [Project[_]]
    p.projectClassFilesCount should be (23)
    p.allMethodsWithBody.size should not be (0)
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

    val port = new SecureRandom().nextInt(10000) + 10000

    val f = Http().bindAndHandle(Route.handlerFlow(route), "0.0.0.0", port )

    Await.ready(Future { Thread.sleep(100); true}, 150.milliseconds)
    val target = new URL(s"http://127.0.0.1:$port/sootconfig-1.1.jar")
    val conn = target.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    conn.setRequestProperty("User-Agent", s"Delphi Maven-Indexer-Reader/1.0.0-SNAPSHOT")

    val bufferedInputStream = new BufferedInputStream(conn.getInputStream)
    val jis = new JarInputStream(bufferedInputStream)

    val p = new ClassStreamReader {}.createProject(target, jis)

    p shouldBe a [Project[_]]
    p.projectClassFilesCount should be (23)
    p.allMethodsWithBody.size should not be (0)
  }

  it should "be able to construct classes out of a somewhat broken JAR file" in {
    val target = File("src/test/resources/xsddoc-0.8.jar")
    val fis = target.inputStream()


    val jis = new JarInputStream(fis)

    val p = new ClassStreamReader {}.createProject(target.toURL, jis)

    p shouldBe a [Project[_]]
    p.projectClassFilesCount should be (21)
    p.allMethodsWithBody.size should not be (0)
  }
}
