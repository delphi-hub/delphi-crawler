package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.DispatchActor.{DownloadJar, DownloadPom}

class DispatchActor(ref: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case DownloadJar(id: MavenIdentifier) => {
      log.info("Downloading jar file " + id.artifactId)
      val downloader=new MavenDownloader(id)
      ref forward downloader.downloadJar()
    }
    case DownloadPom(id: MavenIdentifier) => {
      log.info("Downloading pom file " + id.artifactId)
      val downloader=new MavenDownloader(id)
      ref forward downloader.downloadPom()
    }
  }

}

object DispatchActor {
  def props(actorRef:ActorRef) = Props(new DispatchActor(actorRef))

  case class DownloadJar(id: MavenIdentifier)

  case class DownloadPom(id: MavenIdentifier)

}