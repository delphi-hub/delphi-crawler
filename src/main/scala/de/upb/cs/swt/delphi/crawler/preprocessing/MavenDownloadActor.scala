package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

class MavenDownloadActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m : MavenIdentifier => {
      val downloader = new MavenDownloader(m)
      val jar = downloader.downloadJar()
      val pom = downloader.downloadPom()
      sender() ! MavenArtifact(m, jar, pom)
    }
  }
}
object MavenDownloadActor {
  def props = Props(new MavenDownloadActor)
}

