package de.upb.cs.swt.delphi.crawler.processing

import java.io.FileNotFoundException
import java.util.jar.JarInputStream
import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.{MappedEdge, unresMCtoStr}
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.opalj.br.DeclaredMethod

import scala.util.Try

/*
 * This class uses static analysis to match unresolved method calls to dependencies using Maven.
 *
 * All dependencies are loaded into OPAL, then all each method is tested to see if it resolves in that
 * dependency's scope. If it does, it is marked as belonging to that dependency.
 */

class MavenEdgeMappingActor(configuration: Configuration) extends Actor with ActorLogging{
  override def receive: Receive = {
    case (mx: Set[DeclaredMethod], ix: Set[MavenIdentifier]) => {
      Try(matchEdges(mx, ix)) match {
        case util.Success(ex) => sender() ! ex
        case util.Failure(e) => {
          log.warning("Maven mapper threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
    }
  }

  def edgeSearch(edgeSet: Set[DeclaredMethod], mavenList: List[MavenIdentifier]): Set[MappedEdge] = {
    if (edgeSet.isEmpty) {
      Set[MappedEdge]()
    } else {
      mavenList.headOption match {
        case None => {
          log.info("The following unresolved methods could not be mapped to any library:")
          edgeSet.foreach(m => log.info(unresMCtoStr(m)))
          Set[MappedEdge]()
        }
        case Some(id) => {
          Try {
            val library = loadProject(id)
            edgeSet.partition(m => library.resolveMethodReference(m.declaringClassType,
              m.name, m.descriptor).isDefined) match {
              case (hits, misses) => {
                val mappedEdges = hits.map(m => MappedEdge(id, unresMCtoStr(m)))
                mappedEdges ++ edgeSearch(misses, mavenList.tail)
              }
            }
          } match {
            case util.Success(ex) => ex
            case util.Failure(e: FileNotFoundException) => {
              log.info("The Maven coordinates '{}' (listed as a dependency) are invalid", id.toString)
              edgeSearch(edgeSet, mavenList.tail)
            }
            case util.Failure(e: IllegalArgumentException) => {
              log.info("The Maven coordinates '{}' (listed as a dependency) could not be interpreted", id.toString)
              edgeSearch(edgeSet, mavenList.tail)
            }
            case util.Failure(e) => {
              log.info("The analysis of dependency {} threw exception {}", id.toString, e)
              throw e
            }
          }
        }
      }
    }
  }

  def loadProject(identifier: MavenIdentifier) = {
    val jarFile = new MavenDownloader(identifier).downloadJar()
    val project = new ClassStreamReader {}.createProject(jarFile.url, new JarInputStream(jarFile.is))
    project
  }

  private def matchEdges(edgeSet: Set[DeclaredMethod], mavenSet: Set[MavenIdentifier]): Set[MappedEdge] = {
    edgeSearch(edgeSet, mavenSet.toList)
  }
}

object MavenEdgeMappingActor {
  def props(configuration: Configuration) = Props(new MavenEdgeMappingActor(configuration))
}
