package de.upb.cs.swt.delphi.crawler.tools

import java.io.{ByteArrayInputStream, EOFException, InputStream}
import java.util.jar.{JarEntry, JarInputStream}

import scala.collection.mutable

/**
  * Unpacks a stream containing a JAR file in memory.
  * The input stream can be constructed from a file or from a HTTP resource.
  *
  * @param stream A stream containing a jar file.
  */
class JarStreamReader(stream: InputStream) {

  /**
    * Reads the input stream fully and returns a list of tuples with the entry name and its respective input stream.
    *
    * @return A list of tuples with the name of the jar entry and the corresponding stream.
    */
  def readFully(): Traversable[(String, InputStream)] = {
    val jis: JarInputStream = new JarInputStream(stream)
    var je: JarEntry = null

    var entryStreams = mutable.ArrayBuffer.empty[(String, InputStream)]

    while ( {
      (je = jis.getNextJarEntry());
      je != null
    }) {
      val entryName = je.getName
      if(je.getSize.toInt > 0) {
        val entryBytes = new Array[Byte](je.getSize.toInt)

        var remaining = entryBytes.length
        var offset = 0
        while (remaining > 0) {
          val iteration = jis.read(entryBytes, offset, remaining)
          if (iteration < 0) throw new EOFException()
          remaining -= iteration
          offset += iteration
        }

        entryStreams += ((entryName, new ByteArrayInputStream(entryBytes)))
      }
    }
    jis.close()

    entryStreams
  }
}
