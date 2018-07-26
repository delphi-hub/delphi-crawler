package de.upb.cs.swt.delphi.crawler.tools

import java.io._
import java.net.URL
import java.util.jar.{JarEntry, JarInputStream}

import com.typesafe.config.Config
import org.opalj.br.ClassFile
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8LibraryFramework
import org.opalj.log.GlobalLogContext

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Adapts OPAL to allow reading from HTTP resources directly
  *
  * @author Ben Hermann
  * @author Michael Eichberg
  */
trait ClassStreamReader {

  /**
    * Reifies classes in a provided JAR from any origin
    * @param in An input stream of a JAR file
    * @return A list of named reified class files including bodies
    */
  def readClassFiles(in: => JarInputStream,
                     reader : Java8LibraryFramework = Project.JavaClassFileReader(GlobalLogContext, org.opalj.br.BaseConfig))
                  : List[(ClassFile, String)] = org.opalj.io.process(in) { in =>
    var je: JarEntry = in.getNextJarEntry()

    var futures: List[Future[List[(ClassFile, String)]]] = Nil

    while (je != null) {
      val entryName = je.getName
      if (je.getSize.toInt > 0 && entryName.endsWith(".class")) {
        val entryBytes = new Array[Byte](je.getSize.toInt)

        var remaining = entryBytes.length
        var offset = 0
        while (remaining > 0) {
          val readBytes = in.read(entryBytes, offset, remaining)
          if (readBytes < 0) throw new EOFException()
          remaining -= readBytes
          offset += readBytes
        }
        futures ::= Future[List[(ClassFile, String)]] {
          val cfs = reader.ClassFile(new DataInputStream(new ByteArrayInputStream(entryBytes)))
          cfs map { cf => (cf, entryName) }
        }(org.opalj.concurrent.OPALExecutionContext)
      }
      je = in.getNextJarEntry()
    }

    futures.flatMap(f => Await.result(f, Duration.Inf))
  }

  /**
    * Creates a OPAL Project from a jar input stream
    * @param source The source of the JAR file
    * @param jarInputStream An input stream for the JAR file
    * @return An OPAL Project including the JRE as library classes
    */
  def createProject(source: URL, jarInputStream: JarInputStream): Project[URL] = {
    val config : Config = org.opalj.br.BaseConfig

    val projectClasses: Traversable[(ClassFile, URL)] = readClassFiles(jarInputStream).map(c => (c._1, source))
    val libraryClasses: Traversable[(ClassFile, URL)] =
      readClassFiles(new JarInputStream
                            (new FileInputStream(org.opalj.bytecode.RTJar)), Project.JavaLibraryClassFileReader)
        .map(c => (c._1, org.opalj.bytecode.RTJar.toURI.toURL))


    Project(projectClasses, libraryClasses, true, Traversable.empty)(config, OPALLogAdapter)
  }
}
