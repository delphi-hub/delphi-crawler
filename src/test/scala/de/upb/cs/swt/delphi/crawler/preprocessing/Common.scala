// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
      len = stream.read(bytes)
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
