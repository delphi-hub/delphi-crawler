package de.upb.cs.swt.delphi.crawler.preprocessing

import java.io.InputStream

import scala.concurrent.ExecutionContext.Implicits.global
import java.net.URI

import de.upb.cs.swt.delphi.crawler.BuildInfo
import de.upb.cs.swt.delphi.crawler.discovery.maven.{HttpResourceHandler, MavenIdentifier}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

import scala.concurrent.Future
import scala.io.Source

class MavenDownloader(identifier: MavenIdentifier) {
  val http = new HttpResourceHandler(constructArtifactBaseUri())
  val pomResource = http.locate(pomFilename(identifier))
  val jarResource = http.locate(jarFilename(identifier))

  /**
    * Construct url from maven identifier
    * @return Base URI
    */
  def constructArtifactBaseUri(): URI =
    new URI(identifier.repository)
      .resolve(identifier.groupId.replace('.', '/') + "/")
      .resolve(identifier.artifactId + "/")
      .resolve(identifier.version + "/")


  def pomFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".pom"

  def jarFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".jar"

  def downloadJar(): JarFile = {
    JarFile(pomResource.read())
  }

  def downloadPom(): PomFile= {
    PomFile(pomResource.read())
  }
}
