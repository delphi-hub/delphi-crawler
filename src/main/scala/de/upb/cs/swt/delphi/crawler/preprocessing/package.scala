package de.upb.cs.swt.delphi.crawler

import java.io.InputStream


/**
  * @author Hariharan.
  */
package object preprocessing {

  case class JarFile(is: InputStream)

  case class PomFile(is: InputStream)

}
