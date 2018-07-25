name := "delphi-crawler"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val crawler = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(DockerPlugin).
  settings (
    dockerBaseImage := "openjdk:jre-alpine"
  ).
  enablePlugins(AshScriptPlugin).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.upb.cs.swt.delphi.crawler"
  )

mainClass in (Compile, run) := Some("de.upb.cs.swt.delphi.crawler.Crawler")
mainClass in (Compile, packageBin) := Some("de.upb.cs.swt.delphi.crawler.Crawler")
mainClass in Compile :=  Some("de.upb.cs.swt.delphi.crawler.Crawler")

val akkaVersion = "2.4.20"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.11"
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

val elastic4sVersion = "6.2.8"
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

// Pinning specific libraries b/c of vulnerabilities
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.6"
libraryDependencies += "com.google.guava" % "guava" % "25.1-jre"