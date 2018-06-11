package de.upb.cs.swt.delphi.crawler.preprocessing

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.file.{Files, Paths}

/**
  * @author Hariharan.
  */
object Common {
  def inputStreamToBytes(stream: InputStream): Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val bytes = new Array[Byte] (4096)
    var len: Int = 0
    while ( {
      len = stream.read(bytes);
      len != -1
    }) {
      buffer.write(bytes, 0, len)
    }
    buffer.flush()
    buffer.toByteArray
  }
  def checkJar(is:InputStream):Unit={
    val jarBytes = inputStreamToBytes(is)
    val tmpDir = System.getProperty("java.io.tmpdir")
    val jarPath = Paths.get(tmpDir).resolve("junit.jar")
    Files.write(jarPath, jarBytes)
    assert(jarPath.toFile.exists())
    assert(jarPath.toFile.length() > 0)
  }
  def checkPom(is:InputStream):Unit={
    val pomBytes = inputStreamToBytes(is)
    val tmpDir = System.getProperty("java.io.tmpdir")
    val pomPath = Paths.get(tmpDir).resolve("pom.xml")
    Files.write(pomPath, pomBytes)
    assert(pomPath.toFile.exists())
    assert(pomPath.toFile.length() > 0)
  }
}
