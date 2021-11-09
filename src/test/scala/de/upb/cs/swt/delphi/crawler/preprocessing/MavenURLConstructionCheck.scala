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


import de.upb.cs.swt.delphi.core.model.MavenIdentifier
import org.scalatest.{FlatSpec, Matchers}

class MavenURLConstructionCheck extends FlatSpec with Matchers {
  "Regular identifiers" should "create regular urls" in {
    val reg1 = MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "log4j", "log4j", Some("1.2.9"))
    val reg2 = MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "de.tu-darmstadt.stg", "sootkeeper", Some("1.0"))
    val reg3 = MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "de.tuebingen.uni.sfs.germanet", "germanet-api", Some("13.1.0"))

    reg1.toJarLocation.toString shouldBe "https://repo1.maven.org/maven2/log4j/log4j/1.2.9/log4j-1.2.9.jar"
    reg2.toJarLocation.toString shouldBe "https://repo1.maven.org/maven2/de/tu-darmstadt/stg/sootkeeper/1.0/sootkeeper-1.0.jar"
    reg3.toJarLocation.toString shouldBe "https://repo1.maven.org/maven2/de/tuebingen/uni/sfs/germanet/germanet-api/13.1.0/germanet-api-13.1.0.jar"

    reg1.toPomLocation.toString shouldBe "https://repo1.maven.org/maven2/log4j/log4j/1.2.9/log4j-1.2.9.pom"
    reg2.toPomLocation.toString shouldBe "https://repo1.maven.org/maven2/de/tu-darmstadt/stg/sootkeeper/1.0/sootkeeper-1.0.pom"
    reg3.toPomLocation.toString shouldBe "https://repo1.maven.org/maven2/de/tuebingen/uni/sfs/germanet/germanet-api/13.1.0/germanet-api-13.1.0.pom"
  }


  "Irregular identifiers" should "create escaped urls" in {
    val reg1 = MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "net.java.dev.jogl", "jogl", Some("${parent.version}"))
    val reg2 = MavenIdentifier(Some("https://repo1.maven.org/maven2/"), "com.sun.corba", "glassfish-corba-orbgeneric",Some("${release.version}-${build.version}"))

    reg1.toJarLocation.toASCIIString shouldBe "https://repo1.maven.org/maven2/net/java/dev/jogl/jogl/%24%7Bparent.version%7D/jogl-%24%7Bparent.version%7D.jar"
    reg2.toJarLocation.toASCIIString shouldBe "https://repo1.maven.org/maven2/com/sun/corba/glassfish-corba-orbgeneric/%24%7Brelease.version%7D-%24%7Bbuild.version%7D/glassfish-corba-orbgeneric-%24%7Brelease.version%7D-%24%7Bbuild.version%7D.jar"

    reg1.toPomLocation.toASCIIString shouldBe "https://repo1.maven.org/maven2/net/java/dev/jogl/jogl/%24%7Bparent.version%7D/jogl-%24%7Bparent.version%7D.pom"
    reg2.toPomLocation.toASCIIString shouldBe "https://repo1.maven.org/maven2/com/sun/corba/glassfish-corba-orbgeneric/%24%7Brelease.version%7D-%24%7Bbuild.version%7D/glassfish-corba-orbgeneric-%24%7Brelease.version%7D-%24%7Bbuild.version%7D.pom"
  }


}
