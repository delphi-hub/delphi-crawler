package de.upb.cs.swt.delphi.crawler.tools

import java.io.{FileInputStream, InputStream}
import java.net.URL

import com.typesafe.config.Config
import org.opalj.br.ClassFile
import org.opalj.br.analyses.Project
import org.opalj.log.{GlobalLogContext, OPALLogger}

/**
  * Reifies Java classes from a list of (name, inputStream) tuples.
  *
  * @author Ben Hermann
  */
trait ClassStreamReader {
  /**
    * Reifies Java classes from a list of (name, inputStream) tuples.
    *
    * @param jarEntryStream A list of (name, inputStream) tuples.
    * @return A list of class files found in the stream.
    */
  def reifyClasses(jarEntryStream: Traversable[(String, InputStream)]): Traversable[ClassFile] = {
    val config : Config = org.opalj.br.BaseConfig

    val reader = Project.JavaClassFileReader(GlobalLogContext, config)
    jarEntryStream.filter(e => e._1.endsWith(".class"))
      .flatMap(e => reader.ClassFile(() => e._2))
  }

  /**
    * Creates a OPAL Project from a list of named InputStreams
    * @param source The source of the JAR file
    * @param jarEntryStream The converted list of InputStreams of classes with their entry names
    * @return An OPAL Project including the JRE as library classes
    */
  def createProject(source: URL, jarEntryStream: Traversable[(String, InputStream)]): Project[URL] = {
    val config : Config = org.opalj.br.BaseConfig

    val projectClasses: Traversable[(ClassFile, URL)] = reifyClasses(jarEntryStream).map(c => (c, source))
    val libraryClasses: Traversable[(ClassFile, URL)] =
      reifyClasses(new JarStreamReader(new FileInputStream(org.opalj.bytecode.RTJar)).readFully())
        .map(c => (c, org.opalj.bytecode.RTJar.toURI.toURL))


    Project(projectClasses, libraryClasses, true, Traversable.empty)(config, OPALLogAdapter)
  }
}
