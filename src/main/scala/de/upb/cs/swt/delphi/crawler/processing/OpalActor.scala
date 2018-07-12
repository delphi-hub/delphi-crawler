package de.upb.cs.swt.delphi.crawler.processing

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.JarFile
import de.upb.cs.swt.delphi.crawler.processing.OpalActor.TempFile
import org.apache.commons.io.FileUtils
import org.opalj.AnalysisModes
import org.opalj.br.analyses.{AnalysisModeConfigFactory, Project}
import org.opalj.ai.analyses.cg.{CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}
import org.opalj.ai.analyses.cg.CallGraphFactory.defaultEntryPointsForLibraries

class OpalActor(configuration: Configuration) extends Actor with ActorLogging{

  override def receive: Receive = {
    case TempFile(f) =>
      findCalls(f)
    case JarFile(is) => {   //TODO: Remove this before this goes to production - ideally when we can pass IS to OPAL
      val dummyFile: File = new File(configuration.tempFileStorage + "testFile-" + System.nanoTime() + ".jar")
      FileUtils.copyInputStreamToFile(is, dummyFile)
      is.close()

      findCalls(dummyFile)

      dummyFile.delete()
    }
  }

  private def findCalls(file: File) = {
    val p = Project(file, org.opalj.bytecode.RTJar)
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