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

name := "delphi-crawler"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val crawler = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(DockerPlugin).
  settings (
    dockerBaseImage := "openjdk:jre-alpine"
  ).
  enablePlugins(ScalastylePlugin).
  enablePlugins(AshScriptPlugin).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.upb.cs.swt.delphi.crawler"
  )


scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

mainClass in (Compile, run) := Some("de.upb.cs.swt.delphi.crawler.Crawler")
mainClass in (Compile, packageBin) := Some("de.upb.cs.swt.delphi.crawler.Crawler")
mainClass in Compile :=  Some("de.upb.cs.swt.delphi.crawler.Crawler")

val akkaVersion = "2.5.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.3"
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime

val elastic4sVersion = "6.3.0"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,

  // for the http client
  "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,

  // if you want to use reactive streams
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,

  // testing
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test",
  "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion % "test"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val opalVersion = "1.0.0"
libraryDependencies ++= Seq(
  "de.opal-project" % "common_2.12" % opalVersion,
  "de.opal-project" % "opal-developer-tools_2.12" % opalVersion
)

val mavenVersion = "3.5.2"
libraryDependencies ++= Seq (
  "org.apache.maven" % "maven-core" % mavenVersion,
  "org.apache.maven" % "maven-model" % mavenVersion,
  "org.apache.maven" % "maven-repository-metadata" % mavenVersion,
  "org.apache.maven" % "maven-resolver-provider" % mavenVersion
)

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "1.0.1",
  "io.get-coursier" %% "coursier-cache" % "1.0.1"
)

libraryDependencies += "org.apache.maven.indexer" % "indexer-reader" % "6.0.0"
libraryDependencies += "org.apache.maven.indexer" % "indexer-core" % "6.0.0"

// Pinning secure versions of insecure transitive libraryDependencies
// Please update when updating dependencies above (including Play plugin)
libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % "25.1-jre"
)
