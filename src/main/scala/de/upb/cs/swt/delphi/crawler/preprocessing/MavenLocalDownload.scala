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

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.{Files, Paths}


/**
  * @author Seena Mathew.
  */

object MavenLocalDownload {
  val tmpDir: String = System.getProperty("java.io.tmpdir")

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

  def tempDownloadJar(m : MavenArtifact, tempFile: File) : String={
    val isJar = m.jarFile.is
    val jarBytes = inputStreamToBytes(isJar)
    val jarPathCheck = Paths.get(tmpDir).resolve(tempFile.getName)
    Files.write(jarPathCheck, jarBytes)
    jarPathCheck.toString
  }
}
