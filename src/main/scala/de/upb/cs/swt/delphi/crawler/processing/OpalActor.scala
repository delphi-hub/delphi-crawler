package de.upb.cs.swt.delphi.crawler.processing

import java.io.{BufferedInputStream, File, InputStream}
import java.net.URL
import java.util.jar.JarInputStream

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.JarFile
import de.upb.cs.swt.delphi.crawler.processing.OpalActor.TempFile
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.apache.commons.io.FileUtils
import org.opalj.AnalysisModes
import org.opalj.br.analyses.{AnalysisModeConfigFactory, InstantiableClassesKey, Project}
import org.opalj.ai.analyses.cg.{CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}
import org.opalj.ai.analyses.cg.CallGraphFactory.defaultEntryPointsForLibraries

/*
 * This class uses static analysis to determine which methods in a project belong to external libraries.
 *
 * OPAL is used to create a call graph for each library, and any unresolvable method calls are assumed to
 * belong to some other library.
 */

class OpalActor(configuration: Configuration) extends Actor with ActorLogging{

  override def receive: Receive = {
    case JarFile(is, url) => {
      try {
        findCalls(is, url)
      } catch {
        case e: Exception => {
          log.warning("Opal actor threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
    }
  }

  private def findCalls(is: InputStream, url: URL) = {
    val p = new ClassStreamReader {}.createProject(url, new JarInputStream(is))
    is.close()
    val cpaP = AnalysisModeConfigFactory.resetAnalysisMode(p, AnalysisModes.OPA)
    val entryPoints = () => defaultEntryPointsForLibraries(cpaP)
    val config = new ExtVTACallGraphAlgorithmConfiguration(cpaP)
    val callGraph = CallGraphFactory.create(cpaP, entryPoints, config)

    val libraryCalls = callGraph.unresolvedMethodCalls.toSet

    sender() ! libraryCalls
  }
}

object OpalActor{
  def props(configuration: Configuration) = Props(new OpalActor(configuration))

  case class TempFile(file: File)
}