package de.upb.cs.swt.delphi.crawler.discovery.maven

import java.io.InputStream
import java.net.{HttpURLConnection, URI}

import de.upb.cs.swt.delphi.crawler.BuildInfo
import org.apache.maven.index.reader.ResourceHandler

class HttpResourceHandler(root : URI) extends ResourceHandler {
  override def locate(name: String): ResourceHandler.Resource = new HttpResource(name)

  override def close(): Unit = {}

  class HttpResource(name: String) extends ResourceHandler.Resource {
    override def read(): InputStream = {
      import java.io.BufferedInputStream
      val target = root.resolve(name).toURL

      val conn = target.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("GET")
      conn.setRequestProperty("User-Agent", s"Delphi Maven-Indexer-Reader/${BuildInfo.version}" )

      new BufferedInputStream(conn.getInputStream)
    }
  }

}
