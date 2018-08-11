package de.upb.cs.swt.delphi.crawler.discovery.maven

import java.net.URI

import de.upb.cs.swt.delphi.crawler.Identifier

case class MavenIdentifier(val repository: String, val groupId: String, val artifactId: String, val version: String) extends Identifier {
  def toUniqueString = {
    repository + ":" + groupId + ":" + artifactId + ":" + version
  }

  def toJarLocation : URI = {
    constructArtifactBaseUri().resolve(artifactId + "-" + version + ".jar")
  }

  def toPomLocation : URI = {
    constructArtifactBaseUri().resolve(artifactId + "-" + version + ".pom")
  }

  private def constructArtifactBaseUri(): URI =
    new URI(repository)
      .resolve(groupId.replace('.', '/') + "/")
      .resolve(artifactId + "/")
      .resolve(version + "/")
}
