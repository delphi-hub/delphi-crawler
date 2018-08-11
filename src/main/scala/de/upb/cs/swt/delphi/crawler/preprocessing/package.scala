package de.upb.cs.swt.delphi.crawler

import java.io.InputStream


/**
  * @author Hariharan.
  */
package object preprocessing {

  /**
    * Used for identifcation (Pattern matching) of jar file
    * @param is jar file stream
    */
  case class JarFile(is: InputStream)

  /**
    * Used for identification (Pattern matching) of pom file
    * @param is pom file stream
    */
  case class PomFile(is: InputStream)

}
