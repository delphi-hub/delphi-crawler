package de.upb.cs.swt.delphi.crawler.tools

import java.io.BufferedInputStream

import org.opalj.br.analyses.Project
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.io.File

/**
  * @author Ben Hermann
  */
class ClassStreamReaderTest extends FlatSpec with Matchers {
  "A ClassStreamReader" should "be able to reify classes from sootconfig-1.1" in {
    val fis = File("src/test/resources/sootconfig-1.1.jar").inputStream()
    val jsr = new JarStreamReader(new BufferedInputStream(fis))
    val entries = jsr.readFully()

    val classes = new ClassStreamReader {}.reifyClasses(entries)

    classes.size should be (23)
  }

  "A ClassStreamReader" should "be able to produce a useful OPAL Project instance" in {
    val file = File("src/test/resources/sootconfig-1.1.jar")
    val fis = file.inputStream()
    val jsr = new JarStreamReader(new BufferedInputStream(fis))
    val entries = jsr.readFully()

    val p = new ClassStreamReader {}.createProject(file.toURL, entries)

    p shouldBe a [Project[_]]
    p.projectClassFilesCount should be (23)
    p.allMethodsWithBody.size should not be (0)
  }
}
