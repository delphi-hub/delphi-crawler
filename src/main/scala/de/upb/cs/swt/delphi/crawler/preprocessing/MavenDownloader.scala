package de.upb.cs.swt.delphi.crawler.preprocessing

import java.net.URI

import de.upb.cs.swt.delphi.crawler.discovery.maven.{HttpResourceHandler, MavenIdentifier}

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
    JarFile(jarResource.read())
  }

  def downloadPom(): PomFile= {
    PomFile(pomResource.read())
  }
}
