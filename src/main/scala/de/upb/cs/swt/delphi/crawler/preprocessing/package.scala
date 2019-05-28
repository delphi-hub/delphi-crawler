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
