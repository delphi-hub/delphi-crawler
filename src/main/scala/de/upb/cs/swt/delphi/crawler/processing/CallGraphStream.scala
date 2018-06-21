package de.upb.cs.swt.delphi.crawler.processing

import java.io.File

import akka.NotUsed
import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Unzip, Zip, ZipWith}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.util.Timeout
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, PomFile}
import org.apache.maven.model.Dependency
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.cg.cha.CHACallGraphKey

import scala.concurrent.duration._
import scala.collection.JavaConverters._

class CallGraphStream(opalActor: ActorRef, configuration: Configuration) {

  implicit val timeout: Timeout = 15 seconds
  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()

  val edgeSetFlow: Flow[JarFile, Set[Method], NotUsed] = Flow[JarFile].mapAsync[Set[Method]](1)((jf: JarFile) => (opalActor ? jf).mapTo[Set[Method]])
  val dependencyConverter: Flow[PomFile, Set[MavenIdentifier], NotUsed] = Flow.fromFunction(mavenDependencyConverter)

  val esEdgeMatcher: Flow[(Set[Method], Set[MavenIdentifier]), ((Set[Method], Set[MavenIdentifier]), Set[MappedEdge]), NotUsed] = ???
  val mavenEdgeMatcher: Flow[(Set[Method], Set[MavenIdentifier]), Set[MappedEdge], NotUsed] = ???

  val edgeGeneratingGraph: Flow[(JarFile, PomFile), (Set[Method], Set[MavenIdentifier]), NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    val unzipper = b.add(Unzip[JarFile, PomFile])
    val zipper = b.add(Zip[Set[Method], Set[MavenIdentifier]])

    unzipper.out0 ~> edgeSetFlow ~> zipper.in0
    unzipper.out1 ~> dependencyConverter ~> zipper.in1

    FlowShape(unzipper.in, zipper.out)
  })

  val edgeMatchingGraph: Flow[(Set[Method], Set[MavenIdentifier]), Set[MappedEdge], NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    val unzipper = b.add(Unzip[(Set[Method], Set[MavenIdentifier]), Set[MappedEdge]])
    val merger = b.add(ZipWith[Set[MappedEdge], Set[MappedEdge], Set[MappedEdge]]((s1, s2) => s1 ++ s2))

    esEdgeMatcher.shape ~> unzipper.in
      unzipper.out0 ~> mavenEdgeMatcher ~> merger.in0
      unzipper.out1 ~> merger.in1

    FlowShape(esEdgeMatcher.shape.in, merger.out)
  })

  def mavenDependencyConverter(pomFile: PomFile): Set[MavenIdentifier] = pomReader.read(pomFile.is).getDependencies()
    .asScala.toSet[Dependency].map(d =>
      MavenIdentifier(
        configuration.mavenRepoBase.toString(),
        d.getGroupId(),
        d.getArtifactId(),
        d.getVersion()
      ))

  case class MappedEdge(library: MavenIdentifier, method: String)
}
