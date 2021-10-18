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

package de.upb.cs.swt.delphi.crawler.tools

import java.io.BufferedInputStream
import java.net.{HttpURLConnection, URL}
import java.security.SecureRandom
import java.util.jar.JarInputStream
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{get, getFromFile, path}
import akka.http.scaladsl.server.Route
import org.opalj.br.analyses.Project
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.io.File

/**
  * @author Ben Hermann
  */
class ClassStreamReaderTest extends AnyFlatSpec with should.Matchers with BeforeAndAfter {

  before {
    OPALLogAdapter.setOpalLoggingEnabled(false)
  }

  "A ClassStreamReader" should "be able to produce a useful OPAL Project instance" in {
    val file = File("src/test/resources/sootconfig-1.1.jar")

    val p = ClassStreamReader.createProject(file.toURL, projectIsLibrary = true)

    p shouldBe a[Project[_]]
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
    implicit val ec : ExecutionContext = system.dispatcher

    val port = new SecureRandom().nextInt(10000) + 10000

    Http().bindAndHandle(Route.handlerFlow(route), "0.0.0.0", port )

    Await.ready(Future { Thread.sleep(100); true}, 150.milliseconds)
    val target = new URL(s"http://127.0.0.1:$port/sootconfig-1.1.jar")
    val conn = target.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    conn.setRequestProperty("User-Agent", s"Delphi Maven-Indexer-Reader/1.0.0-SNAPSHOT")

    val bufferedInputStream = new BufferedInputStream(conn.getInputStream)
    val jis = new JarInputStream(bufferedInputStream)

    val p = ClassStreamReader.createProject(target, jis,projectIsLibrary = true)

    p shouldBe a[Project[_]]
    p.projectClassFilesCount should be (23)
    p.allMethodsWithBody.size should not be (0)
  }

  it should "be able to construct classes out of a somewhat broken JAR file" in {
    val target = File("src/test/resources/xsddoc-0.8.jar")

    val p = ClassStreamReader.createProject(target.toURL, projectIsLibrary = true)

    p shouldBe a[Project[_]]
    p.projectClassFilesCount should be (21)
    p.allMethodsWithBody.size should not be (0)
  }
}
