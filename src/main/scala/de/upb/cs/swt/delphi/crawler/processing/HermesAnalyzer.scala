package de.upb.cs.swt.delphi.crawler.processing

import java.io.File
import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.hermes._

/**
  * Custom Hermes runner for Delphi
  *
  * @author Ben Hermann
  *
  */
class HermesAnalyzer(project: Project[URL]) extends HermesCore {



  override def updateProjectData(f: => Unit): Unit = Hermes.synchronized {
    f
  }

  override def reportProgress(f: => Double): Unit = Hermes.synchronized {
    f
  }

  def analyzeProject(): Iterator[(FeatureQuery, TraversableOnce[Feature[URL]])] = {
    for {
      projectFeatures <- featureMatrix.iterator
      projectConfiguration = projectFeatures.projectConfiguration
      (featureQuery, features) <- projectFeatures.featureGroups.par
    } yield {
      val features = featureQuery(projectConfiguration, project, Nil)
      (featureQuery, features)
    }
  }

  override lazy val projectConfigurations: List[ProjectConfiguration] = {
    val projectSource: Option[URL] = project.projectClassFilesWithSources.headOption.map { case (cf, source) => source }
    val librarySource: Option[URL] = project.libraryClassFilesWithSources.headOption.map { case (cf, source) => source }
    List(ProjectConfiguration("project", projectSource.toString, Some(librarySource.toString), None))
  }

  // TODO: At some point we might want to make that uniformly configurable
  override lazy val registeredQueries: List[Query] = {
    List(
      "org.opalj.hermes.queries.BytecodeInstructions",
      "org.opalj.hermes.queries.ClassFileVersion",
      "org.opalj.hermes.queries.SizeOfInheritanceTree",
      "org.opalj.hermes.queries.ClassTypes"
    ).map { s => Query(s, true) }
  }
}

object HermesAnalyzer extends HermesCore {

  def setConfig() = {
    // TODO: Make this much nicer... Fake File?
    initialize(new File("src/main/resources/application.conf"))
  }

  override def updateProjectData(f: => Unit): Unit = Hermes.synchronized {
    f
  }

  override def reportProgress(f: => Double): Unit = Hermes.synchronized {
    f
  }
}


