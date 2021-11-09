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

package de.upb.cs.swt.delphi.crawler.tools

import java.io._
import java.net.URL
import java.util.jar.{JarEntry, JarInputStream}
import com.typesafe.config.{Config, ConfigValueFactory}
import org.opalj.br.{BaseConfig, ClassFile}
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8LibraryFramework

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * Adapts OPAL to allow reading from HTTP resources directly
  *
  * @author Ben Hermann
  * @author Michael Eichberg
  */
object ClassStreamReader {

  /**
    * Reifies classes in a provided JAR from any origin
    *
    * @param in An input stream of a JAR file
    * @return A list of named reified class files including bodies
    */
  def readClassFiles(in: => JarInputStream,
                     reader: Java8LibraryFramework = Project.JavaClassFileReader(OPALLogAdapter.analysisLogContext, org.opalj.br.BaseConfig))
  : List[(ClassFile, String)] = org.opalj.io.process(in) { in =>
    var je: JarEntry = in.getNextJarEntry

    var futures: List[Future[List[(ClassFile, String)]]] = Nil

    while (je != null) {
      val entryName = je.getName
      if (entryName.endsWith(".class")) {

        val entryBytes = {
          val baos = new ByteArrayOutputStream()
          val buffer = new Array[Byte](32 * 1024)

          Stream.continually(in.read(buffer)).takeWhile(_ > 0).foreach { bytesRead =>
            baos.write(buffer, 0, bytesRead)
            baos.flush()
          }

          baos.toByteArray
        }

        futures ::= Future[List[(ClassFile, String)]] {
          val cfs = reader.ClassFile(new DataInputStream(new ByteArrayInputStream(entryBytes)))
          cfs map { cf => (cf, entryName) }
        }(org.opalj.concurrent.OPALUnboundedExecutionContext)
      }
      je = in.getNextJarEntry
    }

    val result = futures.flatMap(f => Await.result(f, 2 minutes))

    result
  }

  /**
    * Creates a OPAL Project from a jar input stream
    *
    * @param source The source of the JAR file
    * @return An OPAL Project including the JRE as library classes
    */
  def createProject(source: URL, projectIsLibrary: Boolean): Project[URL] = {

    val jarFile = new File(source.getFile)
    val jarInputStream = new JarInputStream(new FileInputStream(jarFile))

    createProject(source, jarInputStream, projectIsLibrary)
  }

  def createProject(source: URL, is: InputStream, projectIsLibrary: Boolean): Project[URL] = {
    val jis = new JarInputStream(is)

    createProject(source, jis, projectIsLibrary)
  }

  def createProject(source: URL,
                    jarInputStream: JarInputStream,
                    projectIsLibrary: Boolean): Project[URL] = {

    val config: Config = if (projectIsLibrary) {
      BaseConfig.withValue("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis",
        ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.LibraryEntryPointsFinder"))
    } else {
      BaseConfig
    }

    val projectClasses: Iterable[(ClassFile, URL)] = readClassFiles(jarInputStream).map { case (classFile, _) => (classFile, source) }

    val libraryClasses: List[(ClassFile, URL)] = readClassFiles(new JarInputStream
    (new FileInputStream(org.opalj.bytecode.RTJar)), Project.JavaLibraryClassFileReader)
      .map { case (classFile, _) => (classFile, org.opalj.bytecode.RTJar.toURI.toURL) }

    Project(projectClasses, libraryClasses, libraryClassFilesAreInterfacesOnly = true, Iterable.empty)(config, OPALLogAdapter.analysisLogger)
  }
}
