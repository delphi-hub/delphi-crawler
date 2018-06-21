package de.upb.cs.swt.delphi.crawler.processing

import java.io.{File, FileInputStream, InputStream}

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.preprocessing.JarFile
import de.upb.cs.swt.delphi.crawler.processing.OpalActor.TempFile
import org.apache.commons.io.{FileUtils, IOUtils}
import org.opalj.AnalysisModes
import org.opalj.br.analyses.{AnalysisModeConfigFactory, Project}
import org.opalj.fpcf.analyses.cg.cha.CHACallGraphKey

class OpalActor(configuration: Configuration) extends Actor with ActorLogging{

  override def receive: Receive = {
    case TempFile(f) =>
      val p = Project(f)
      val cpaP = AnalysisModeConfigFactory.resetAnalysisMode(p, AnalysisModes.OPA)
      val callGraph = cpaP.get(CHACallGraphKey).callGraph    //We could use a different call graph algorithm based on our use case
      val projectMethods = cpaP.allProjectClassFiles.flatMap(cf => cf.methods)

      log.info("This project has {} files", cpaP.projectClassFilesCount)
      log.info("The call graph contains {} edges", callGraph.callsCount)

      val libraryCalls = (for (
        caller <- projectMethods;
        callee <- callGraph.calls(caller).flatMap{case (_, method) => method}
        if cpaP.isLibraryType(callee.classFile)
      ) yield callee).toSet

      sender() ! libraryCalls
  }
}

object OpalActor{
  def props(configuration: Configuration) = Props(new OpalActor(configuration))

  case class TempFile(file: File)
}