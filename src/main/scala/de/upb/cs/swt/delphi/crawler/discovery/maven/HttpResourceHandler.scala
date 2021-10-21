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

package de.upb.cs.swt.delphi.crawler.discovery.maven

import java.io.{BufferedInputStream, InputStream}
import java.net.{HttpURLConnection, URI}

import de.upb.cs.swt.delphi.crawler.BuildInfo
import org.apache.maven.index.reader.ResourceHandler
import scala.concurrent.duration._

class HttpResourceHandler(root : URI) extends ResourceHandler {
  override def locate(name: String): ResourceHandler.Resource = new HttpResource(name)

  override def close(): Unit = {}

  class HttpResource(name: String) extends ResourceHandler.Resource {
    override def read(): InputStream = {

      val target = root.resolve(name).toURL

      val conn: HttpURLConnection = target.openConnection.asInstanceOf[HttpURLConnection]
      conn.setReadTimeout(24.hours.toMillis.toInt)
      conn.setRequestMethod("GET")
      conn.setRequestProperty("User-Agent", s"Delphi Maven-Indexer-Reader/${BuildInfo.version}" )

      new BufferedInputStream(conn.getInputStream)
    }
  }

}
