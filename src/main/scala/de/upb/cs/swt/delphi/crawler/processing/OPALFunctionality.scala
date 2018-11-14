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

package de.upb.cs.swt.delphi.crawler.processing
import java.net.URL
import java.util.jar.JarInputStream

import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.opalj.br.analyses.Project

import scala.util.Try

trait OPALFunctionality {

  def reifyProject(m: MavenArtifact): Project[URL] = {
    val project = new ClassStreamReader {}.createProject(m.identifier.toJarLocation.toURL,
      new JarInputStream(m.jarFile.is))
    Try(m.jarFile.is.close())
    project
  }
}
