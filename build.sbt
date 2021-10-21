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

version := "0.9.5-SNAPSHOT"

scalaVersion := "2.12.15"

lazy val crawler = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(DockerPlugin).
  settings(
    dockerBaseImage := "delphihub/jre-alpine-openjfx",
    dockerAlias := com.typesafe.sbt.packager.docker.DockerAlias(None, Some("delphihub"),"delphi-crawler", Some(version.value)),
  ).
  enablePlugins(ScalastylePlugin).
  enablePlugins(AshScriptPlugin).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.upb.cs.swt.delphi.crawler"
  )


scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Compile / run / mainClass := Some("de.upb.cs.swt.delphi.crawler.Crawler")
Compile / packageBin / mainClass := Some("de.upb.cs.swt.delphi.crawler.Crawler")
Compile / mainClass := Some("de.upb.cs.swt.delphi.crawler.Crawler")

val akkaVersion = "2.6.16"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6",
  "com.typesafe.akka" %% "akka-http" % "10.2.6"
)

libraryDependencies += "org.json4s" %% "json4s-jackson" % "4.0.3"

libraryDependencies += "com.pauldijou" %% "jwt-core" % "1.0.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Runtime

val elastic4sVersion = "7.14.1"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-akka" % elastic4sVersion,

  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val opalVersion = "4.0.0"
libraryDependencies ++= Seq(
  "de.opal-project" % "common_2.12" % opalVersion,
  "de.opal-project" % "framework_2.12" % opalVersion,
  "de.opal-project" % "hermes_2.12" % opalVersion
)

val mavenVersion = "3.8.3"
libraryDependencies ++= Seq (
  "org.apache.maven" % "maven-core" % mavenVersion,
  "org.apache.maven" % "maven-model" % mavenVersion,
  "org.apache.maven" % "maven-repository-metadata" % mavenVersion,
  "org.apache.maven" % "maven-resolver-provider" % mavenVersion
)

libraryDependencies += "com.squareup.tools.build" % "maven-archeologist" % "0.0.10"

libraryDependencies += "org.apache.maven.indexer" % "indexer-reader" % "6.0.0"
libraryDependencies += "org.apache.maven.indexer" % "indexer-core" % "6.0.0"

libraryDependencies += "de.upb.cs.swt.delphi" %% "delphi-core" % "0.9.2"
