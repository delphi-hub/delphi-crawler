package de.upb.cs.swt.delphi.crawler.processing

import java.io.BufferedInputStream
import java.util.jar.JarInputStream

import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.reflect.io.File

class HermesTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    HermesAnalyzer.setConfig()
  }

  "Hermes" should "run on sootconfig-1.1" in {
    val file = File("src/test/resources/sootconfig-1.1.jar")
    val fis = file.inputStream()
    val jis = new JarInputStream(new BufferedInputStream(fis))

    val p = new ClassStreamReader {}.createProject(file.toURL, jis)

    val result = new HermesAnalyzer(p).analyzeProject()
    for {
      (query, features) <- result
      feature <- features
    } {
      println(s"${query.id} - ${feature.id.replace("\n", " ")}: ${feature.count}")
    }
  }


}