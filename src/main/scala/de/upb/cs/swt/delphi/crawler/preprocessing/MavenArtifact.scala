package de.upb.cs.swt.delphi.crawler.preprocessing

import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

case class MavenArtifact(identifier : MavenIdentifier, jarFile: JarFile, pomFile: PomFile) {

}
