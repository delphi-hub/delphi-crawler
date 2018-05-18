package de.upb.cs.swt.delphi.crawler.preprocessing

import java.net.URI

import de.upb.cs.swt.delphi.crawler.discovery.maven.{HttpResourceHandler, MavenIdentifier}

trait MavenDownloader {

  def populate(identifier: MavenIdentifier): MavenArtifact = {
    val http = new HttpResourceHandler(constructArtifactBaseUri(identifier))
    val pomResource = http.locate(pomFilename(identifier))
    val jarResource = http.locate(jarFilename(identifier))

    MavenArtifact(identifier)
  }

  def constructArtifactBaseUri(identifier: MavenIdentifier): URI =
    new URI(identifier.repository)
      .resolve(identifier.groupId.replace('.', '/') + "/")
      .resolve(identifier.artifactId + "/")
      .resolve(identifier.version + "/")


  def pomFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".pom"

  def jarFilename(identifier: MavenIdentifier): String =
    identifier.artifactId + "-" + identifier.version + ".jar"
}
