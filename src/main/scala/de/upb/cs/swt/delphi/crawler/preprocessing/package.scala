package de.upb.cs.swt.delphi.crawler

import java.io.InputStream
import java.net.URL


/**
  * @author Hariharan.
  */
package object preprocessing {

  /**
    * Used for identifcation (Pattern matching) of jar file
    * @param is jar file stream
    */
  case class JarFile(is: InputStream, url: URL)

  /**
    * Used for identification (Pattern matching) of pom file
    * @param is pom file stream
    */
  case class PomFile(is: InputStream)

  /**
    * Used for identification (Pattern matching) of metadata file
    * @param is metadata file stream
    */
  case class MetaFile(is: InputStream)

}
