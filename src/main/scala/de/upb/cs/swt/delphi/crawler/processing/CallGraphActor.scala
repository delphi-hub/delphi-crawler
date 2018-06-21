package de.upb.cs.swt.delphi.crawler.processing

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader
import de.upb.cs.swt.delphi.crawler.processing.OpalActor.TempFile
import org.apache.commons.io.FileUtils
import org.opalj.br.Method

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class CallGraphActor(configuration: Configuration) extends Actor with ActorLogging{

  def opalActor = context.actorOf(OpalActor.props(configuration))

  implicit val timeout: Timeout = 30 seconds

  override def receive: Receive = {
    case m: MavenIdentifier =>
      log.info("OpalActor received identifier {}:{}:{}", m.groupId, m.artifactId, m.version)

      val dummyFile: File = new File(configuration.tempFileStorage + "testFile" + m.groupId + ":" + m.artifactId + ":" + m.version)
      FileUtils.copyInputStreamToFile((new MavenDownloader(m).downloadJar()).is, dummyFile)

      val callGraph: Set[Method] = Await.result((opalActor ? TempFile(dummyFile)).mapTo[Set[Method]], 30 seconds)   //TODO: This is a bottleneck, we should pass an ID to the dispatcher so we can
                                                                                      //TODO:   identify and execute the result of this call asynchronously
      log.info("The following {} methods were found in {}:{}:{}:", callGraph.size, m.groupId, m.artifactId, m.version)
      callGraph.foreach(m => log.info(m.fullyQualifiedSignature))

      dummyFile.delete()
  }

}

object CallGraphActor{
  def props(configuration: Configuration) = Props(new CallGraphActor(configuration))

}