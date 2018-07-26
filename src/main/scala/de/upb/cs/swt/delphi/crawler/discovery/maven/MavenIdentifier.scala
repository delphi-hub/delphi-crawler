package de.upb.cs.swt.delphi.crawler.discovery.maven

import de.upb.cs.swt.delphi.crawler.Identifier

case class MavenIdentifier(val repository: String, val groupId: String, val artifactId: String, val version: String) extends Identifier {
  def toUniqueString = {
    repository + ":" + groupId + ":" + artifactId + ":" + version
  }

  override val toString: String = groupId + ":" + artifactId + ":" + version
}
