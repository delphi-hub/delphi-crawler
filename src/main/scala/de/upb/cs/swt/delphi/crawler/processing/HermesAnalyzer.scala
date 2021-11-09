// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.processing

import de.upb.cs.swt.delphi.crawler.processing.HermesAnalyzer.initialize

import java.io._
import java.net.URL
import org.opalj.br.analyses.Project
import org.opalj.hermes._

import scala.collection.JavaConverters.asScalaIteratorConverter

/**
  * Custom Hermes runner for Delphi
  *
  * @author Ben Hermann
  *
  */
class HermesAnalyzer(project: Project[URL]) extends HermesCore {
  initialize(HermesAnalyzer.temporaryConfigFile())


  override def updateProjectData(f: => Unit): Unit = HermesAnalyzer.synchronized {
    f
  }

  override def reportProgress(f: => Double): Unit = HermesAnalyzer.synchronized {
    f
  }

  def analyzeProject(): Iterator[(FeatureQuery, TraversableOnce[Feature[URL]])] = {
    for {
      projectFeatures <- featureMatrix.iterator().asScala
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


  override lazy val registeredQueries: List[Query] = {
    queries.values.flatten.map { s => new Query(s, true) }.toList
  }

  val VERY_SLOW = 'VERY_SLOW
  val SLOW = 'SLOW
  val NOT_SO_SLOW = 'NOT_SO_SLOW
  val OK = 'OK
  val FAST = 'FAST
  val BLAZINGLY_FAST = 'BLAZINGLY_FAST


  val queries = Map(
    VERY_SLOW -> List("org.opalj.hermes.queries.Metrics",
      "org.opalj.hermes.queries.MicroPatterns"),
    SLOW -> List("org.opalj.hermes.queries.FieldAccessStatistics",
      "org.opalj.hermes.queries.TrivialReflectionUsage",
      "org.opalj.hermes.queries.BytecodeInstructions"),
    NOT_SO_SLOW -> List("org.opalj.hermes.queries.RecursiveDataStructures",
      "org.opalj.hermes.queries.MethodsWithoutReturns",
      "org.opalj.hermes.queries.DebugInformation"),
    OK -> List("org.opalj.hermes.queries.FanInFanOut",
      "org.opalj.hermes.queries.GUIAPIUsage",
      "org.opalj.hermes.queries.ClassLoaderAPIUsage",
      "org.opalj.hermes.queries.JavaCryptoArchitectureUsage",
      "org.opalj.hermes.queries.MethodTypes",
      "org.opalj.hermes.queries.ReflectionAPIUsage",
      "org.opalj.hermes.queries.SystemAPIUsage",
      "org.opalj.hermes.queries.ThreadAPIUsage",
      "org.opalj.hermes.queries.UnsafeAPIUsage",
      "org.opalj.hermes.queries.JDBCAPIUsage",
      "org.opalj.hermes.queries.BytecodeInstrumentationAPIUsage"),
    FAST -> List("org.opalj.hermes.queries.ClassTypes"),
    BLAZINGLY_FAST -> List("org.opalj.hermes.queries.SizeOfInheritanceTree",
      "org.opalj.hermes.queries.ClassFileVersion"))
}


object HermesAnalyzer extends HermesCore {

  def setConfig(): Unit = {
    var internalConfigurationFile = temporaryConfigFile()
    initialize(internalConfigurationFile)
  }

  private def temporaryConfigFile() : File = {
    val resourcePath = this.getClass.getClassLoader.getResourceAsStream("hermes.conf")
    val bytes = Stream.continually(resourcePath.read).takeWhile(_ != -1).map(_.toByte).toArray

    val tempConfigFile = File.createTempFile("hermes-", "")
    tempConfigFile.deleteOnExit()

    val bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(tempConfigFile))
    Stream.continually(bufferedOutputStream.write(bytes))
    bufferedOutputStream.close()

    tempConfigFile
  }

  override def updateProjectData(f: => Unit): Unit = HermesAnalyzer.synchronized {
    f
  }

  override def reportProgress(f: => Double): Unit = HermesAnalyzer.synchronized {
    f
  }

  lazy val HermesVersion = {
    classOf[HermesCore].getPackage.getImplementationVersion
  }
}