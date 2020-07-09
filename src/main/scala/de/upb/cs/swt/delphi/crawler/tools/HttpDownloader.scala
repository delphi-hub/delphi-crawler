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

import java.io.{ByteArrayInputStream, InputStream}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import akka.util.ByteString

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

class HttpDownloader(implicit val system: ActorSystem) {

  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def downloadFromUri(requestedUri: String): Try[InputStream] = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = requestedUri))


    Await.result(responseFuture, Duration.Inf) match {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Try(new ByteArrayInputStream(Await.result(entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)))
      case resp@HttpResponse(code, _, _, _) =>
        resp.discardEntityBytes()
        Failure(new HttpException(code))
    }
  }

  def downloadFromUriWithHeaders(requestedUri: String): Try[(InputStream, Seq[HttpHeader])] = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = requestedUri))


    Await.result(responseFuture, Duration.Inf) match {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Try((
          new ByteArrayInputStream(Await.result(entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)),
          headers))
      case resp@HttpResponse(code, _, _, _) =>
        resp.discardEntityBytes()
        Failure(new HttpException(code))
    }
  }
}
