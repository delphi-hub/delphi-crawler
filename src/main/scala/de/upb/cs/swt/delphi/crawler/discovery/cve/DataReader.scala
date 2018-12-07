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

package de.upb.cs.swt.delphi.crawler.discovery.cve

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.zip.ZipInputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

import spray.json._

import de.upb.cs.swt.delphi.crawler.discovery.cve.JsonFormatter._

/**
  * @author Andreas Dann
  * @author Ben Hermann
  * @param streamAddress
  * @param sys
  */
class DataReader(streamAddress: Uri, implicit val sys: ActorSystem) {
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = sys.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri = streamAddress))

  val processFuture: Future[InputStream] = responseFuture.map {
    case HttpResponse(StatusCodes.OK, _, entity, _) => entity.dataBytes.runWith(StreamConverters.asInputStream(20 seconds))
  }

  val is = Await.result(processFuture, Duration.Inf)

  val zipInputStream = new ZipInputStream(is)
  val firstEntry = zipInputStream.getNextEntry()
  val entryBytes = {
    val baos = new ByteArrayOutputStream()
    val buffer = new Array[Byte](32 * 1024)

    Stream.continually(zipInputStream.read(buffer)).takeWhile(_ > 0).foreach { bytesRead =>
      baos.write(buffer, 0, bytesRead)
      baos.flush()
    }

    baos.toByteArray
  }

  val jsonString = new String(entryBytes, "UTF-8")

  val cveEntries = jsonString.parseJson.convertTo[Entries]

  val itemsIt = cveEntries.CVE_Items.iterator

  def read(): Option[Entry] = {
    itemsIt.hasNext match {
      case true => Some(itemsIt.next())
      case false => None
    }
  }

  def close(): Unit = {
    zipInputStream.close()
  }

}
