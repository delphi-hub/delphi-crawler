package de.upb.cs.swt.delphi.crawler.processing

import java.io.File
import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.{MappedEdge, unresMCtoStr}
import org.apache.commons.io.FileUtils
import org.opalj.ai.analyses.cg.UnresolvedMethodCall
import org.opalj.br.analyses.Project

class MavenEdgeMappingActor(configuration: Configuration) extends Actor with ActorLogging{
  override def receive: Receive = {
    case (mx: Set[UnresolvedMethodCall], ix: Set[MavenIdentifier]) => {
      sender() ! matchEdges(mx, ix)
    }
  }

  private def matchEdges(edgeSet: Set[UnresolvedMethodCall], mavenSet: Set[MavenIdentifier]): Set[MappedEdge] = {

    def edgeSearch(edgeSet: Set[UnresolvedMethodCall], mavenList: List[MavenIdentifier]): Set[MappedEdge] = {
      if (edgeSet.isEmpty){
        Set[MappedEdge]()
      } else {
        if (mavenList.isEmpty) {
          log.info("The following unresolved methods could not be mapped to any library:")
          edgeSet.foreach(m => log.info(unresMCtoStr(m)))
          Set[MappedEdge]()
        } else {
          try {
            val identifier: MavenIdentifier = mavenList.head
            val library: Project[URL] = loadProject(identifier)
            val splitSet = edgeSet.partition(m => library.resolveMethodReference(m.calleeClass, m.calleeName, m.calleeDescriptor).isDefined)
            val mappedEdges = splitSet._1.map(m => MappedEdge(identifier, unresMCtoStr(m)))
            mappedEdges ++ edgeSearch(splitSet._2, mavenList.tail)
          } catch {
            case e: java.io.FileNotFoundException =>
              log.info("The Maven coordinates '{}' (listed as a dependency) are invalid", mavenList.head.toString)
              edgeSearch(edgeSet, mavenList.tail)
          }
        }
      }
    }

    def loadProject(identifier: MavenIdentifier): Project[URL] = {
      val jarFile = new MavenDownloader(identifier).downloadJar()
      val dummyFile = new File(configuration.tempFileStorage + identifier.toString + ".jar")
      FileUtils.copyInputStreamToFile(jarFile.is, dummyFile)
      jarFile.is.close()
      val project = Project(dummyFile)

      dummyFile.delete()
      project
    }

    edgeSearch(edgeSet, mavenSet.toList)
  }
}

object MavenEdgeMappingActor {
  def props(configuration: Configuration) = Props(new MavenEdgeMappingActor(configuration))
}
