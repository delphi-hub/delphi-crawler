package de.upb.cs.swt.delphi.crawler.tools

import java.io.BufferedInputStream

import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.io.File

class ClassStreamReaderTest extends FlatSpec with Matchers {
  "A ClassStreamReader" should "be able to reify classes from sootconfig-1.1" in {
    val fis = File("src/test/resources/sootconfig-1.1.jar").inputStream()
    val jsr = new JarStreamReader(new BufferedInputStream(fis))
    val entries = jsr.readFully()

    val classes = new ClassStreamReader {}.reifyClasses(entries)

    classes.size should be (23)
  }
}
