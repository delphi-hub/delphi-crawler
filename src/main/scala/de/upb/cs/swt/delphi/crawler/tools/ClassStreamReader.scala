package de.upb.cs.swt.delphi.crawler.tools

import java.io.InputStream

import org.opalj.br.ClassFile
import org.opalj.br.analyses.Project

/**
  * Reifies Java classes from a list of (name, inputStream) tuples.
  */
trait ClassStreamReader {
  /**
    * Reifies Java classes from a list of (name, inputStream) tuples.
    * @param jarEntryStream A list of (name, inputStream) tuples.
    * @return A list of class files found in the stream.
    */
  def reifyClasses(jarEntryStream: Traversable[(String, InputStream)]): Traversable[ClassFile] = {
    jarEntryStream.filter(e => e._1.endsWith(".class"))
      .flatMap(e => Project.JavaLibraryClassFileReader.ClassFile(() => e._2))
  }

}
