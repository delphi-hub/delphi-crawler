package de.upb.cs.swt.delphi.crawler.processing

import java.io.{File, InputStream}
import java.net.URL
import java.util.jar.JarInputStream
import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.JarFile
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader

import org.opalj.br.VirtualDeclaredMethod
import org.opalj.tac.cg.{CallGraph, XTACallGraphKey}

import scala.util.Try

/*
 * This class uses static analysis to determine which methods in a project belong to external libraries.
 *
 * OPAL is used to create a call graph for each library, and any unresolvable method calls are assumed to
 * belong to some other library.
 */

class OpalActor(configuration: Configuration) extends Actor with ActorLogging{

  override def receive: Receive = {
    case JarFile(is, url) => {
      Try(findCalls(is, url)) match {
        case util.Success(mx) => sender() ! mx
        case util.Failure(e) => {
          log.warning("Opal actor threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
    }
  }

  private def findCalls(is: InputStream, url: URL) = {
    val p = new ClassStreamReader {}.createProject(url, new JarInputStream(is))
    is.close()

    val cg: CallGraph = p.get(XTACallGraphKey)
    cg.reachableMethods().filter{ method =>
      (method.isInstanceOf[VirtualDeclaredMethod] ||
        !p.allProjectClassFiles.exists(_.thisType.equals(method.declaringClassType))) &&
        cg.calleesOf(method).isEmpty
    }.toSet
  }
}

object OpalActor{
  def props(configuration: Configuration) = Props(new OpalActor(configuration))

  case class TempFile(file: File)
}