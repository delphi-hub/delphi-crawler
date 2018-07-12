package de.upb.cs.swt.delphi.crawler.processing


import java.io.{FileNotFoundException, InputStream}

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, Unzip, Zip, ZipWith}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.util.Timeout
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, MavenDownloader, PomFile}
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.MappedEdge
import de.upb.cs.swt.delphi.crawler.storage.ElasticCallGraphActor
import org.apache.maven.model.Dependency
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.Future

class CallGraphStream(configuration: Configuration) extends Actor with ActorLogging {

  override def receive: Receive = {
    case m: MavenIdentifier =>
      graphActor forward m
  }

  implicit val timeout: Timeout = 60 seconds
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val parallelism = 1

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  val esClient = HttpClient(configuration.elasticsearchClientUri)

  val opalActor: ActorRef = context.actorOf(OpalActor.props(configuration))
  val mavenEdgeMapActor: ActorRef = context.actorOf(MavenEdgeMappingActor.props(configuration))
  val esEdgeSearchActor: ActorRef = context.actorOf(ElasticEdgeSearchActor.props(esClient))
  val esPushActor: ActorRef = context.actorOf(ElasticCallGraphActor.props(esClient))

  val fileGenFlow: Flow[MavenIdentifier, (PomFile, (JarFile, MavenIdentifier)), NotUsed] = Flow.fromFunction(fetchFiles)
  val edgeSetFlow: Flow[JarFile, Set[UnresolvedMethodCall], NotUsed] = Flow[JarFile]
    .mapAsync[Set[UnresolvedMethodCall]](parallelism)((jf: JarFile) => (opalActor ? jf).mapTo[Set[UnresolvedMethodCall]])
  val dependencyConverter: Flow[PomFile, Set[MavenIdentifier], NotUsed] = Flow.fromFunction(mavenDependencyConverter)
  val filterFlow: Flow[(Set[MavenIdentifier], (JarFile, MavenIdentifier)), (JarFile, (Set[MavenIdentifier], MavenIdentifier)), NotUsed] =
    Flow.fromFunction{ case (ix, (jf, i)) => (jf, (ix, i))}
  val rearrangeFlow: Flow[(Set[UnresolvedMethodCall], (Set[MavenIdentifier], MavenIdentifier)),
    ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), NotUsed] = Flow.fromFunction{ case (cx, (ix, i)) =>
      ((cx, ix), i) }

  val esEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]), NotUsed] =
    Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier])].mapAsync(parallelism){ case (mx, ix) => (esEdgeSearchActor ? (mx, ix))
      .mapTo[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge])]}
  val mavenEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge], NotUsed] =
    Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier])].mapAsync(parallelism){ case (mx, ix) => (mavenEdgeMapActor ? (mx, ix))
      .mapTo[Set[MappedEdge]]}

  val esPusherSink: Sink[(Set[MappedEdge], MavenIdentifier), Future[Done]] =
    Sink.foreach{ case (ex, i) => (esPushActor ! (i, ex))}

  val edgeGeneratingGraph: Flow[MavenIdentifier, ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>

    val unzipperA = b.add(Unzip[PomFile, (JarFile, MavenIdentifier)])
    val zipperA = b.add(Zip[Set[MavenIdentifier], (JarFile, MavenIdentifier)])
    val unzipperB = b.add(Unzip[JarFile, (Set[MavenIdentifier], MavenIdentifier)])
    val zipperB = b.add(Zip[Set[UnresolvedMethodCall], (Set[MavenIdentifier], MavenIdentifier)])
    val fileGen = b.add(fileGenFlow)
    val rearrange = b.add(rearrangeFlow)

    fileGen.filter(t => (t._2._2.artifactId != null)) ~> unzipperA.in
    unzipperA.out0 ~> dependencyConverter ~> zipperA.in0
    unzipperA.out1 ~> zipperA.in1
    zipperA.out ~> filterFlow.filter{case (_, (ix, i)) =>
      if(ix.isEmpty) { log.info(i.toString + " not mapped, incomplete POM file."); false }
      else { log.info(i.toString + " mapped, valid POM file."); true }} ~> unzipperB.in
    unzipperB.out0 ~> edgeSetFlow ~> zipperB.in0
    unzipperB.out1 ~> zipperB.in1
    zipperB.out ~> rearrange.in

    FlowShape(fileGen.in, rearrange.out)
  })

  val edgeMatchingGraph: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge], NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    val unzipper = b.add(Unzip[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]])
    val merger = b.add(ZipWith[Set[MappedEdge], Set[MappedEdge], Set[MappedEdge]]((s1, s2) => s1 ++ s2))
    val esMatcher = b.add(esEdgeMatcher)

    esMatcher ~> unzipper.in
      unzipper.out0 ~> mavenEdgeMatcher ~> merger.in0
      unzipper.out1 ~> merger.in1

    FlowShape(esMatcher.in, merger.out)
  })

  val edgeMappingGraph: Sink[MavenIdentifier, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit b =>
    val unzipper = b.add(Unzip[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier])
    val zipper = b.add(Zip[Set[MappedEdge], MavenIdentifier])
    val edgeGenerator = b.add(edgeGeneratingGraph)

    edgeGenerator ~> unzipper.in
    unzipper.out0 ~> edgeMatchingGraph ~> zipper.in0
    unzipper.out1 ~> zipper.in1
    zipper.out ~> esPusherSink

    SinkShape(edgeGenerator.in)
  })

  def fetchFiles(mavenIdentifier: MavenIdentifier): (PomFile, (JarFile, MavenIdentifier)) = {
    try {
      val downloader = new MavenDownloader(mavenIdentifier)
      (downloader.downloadPom(), (downloader.downloadJar(), mavenIdentifier))
    } catch {
      case e: FileNotFoundException => {
        log.info("{} not mapped, missing POM file", mavenIdentifier.toString)
        (PomFile(null), (JarFile(null), MavenIdentifier(null, null, null, null)))  //There might be a more elegant way of doing this
      }
    }
  }

  def mavenDependencyConverter(pomFile: PomFile): Set[MavenIdentifier] = {
    val pomSet = pomReader.read(pomFile.is).getDependencies()
    .asScala.toSet[Dependency].map(d =>
      MavenIdentifier(
        configuration.mavenRepoBase.toString(),
        d.getGroupId(),
        d.getArtifactId(),
        d.getVersion()
      ))
    pomFile.is.close()
    pomSet
  }

  val actorSource: Source[MavenIdentifier, ActorRef] = Source.actorRef(5000, OverflowStrategy.dropNew)  //We may need to adjust this

  val printSink: Sink[Set[MappedEdge], Future[Done]] = Sink.foreach[Set[MappedEdge]] { ex =>
    log.info("The following methods were found:")
    ex.foreach{
      e => log.info("In " + e.library.toString + ": " + e.method)
    }
  }

  val callGraphGraph: RunnableGraph[ActorRef] = actorSource.toMat(edgeMappingGraph)(Keep.left)

  val graphActor: ActorRef = callGraphGraph.run()
}

object CallGraphStream{
  def props(configuration: Configuration) = Props(new CallGraphStream(configuration))

  case class MappedEdge(library: MavenIdentifier, method: String)   //I'm not sure if this is the best place to put these
  def unresMCtoStr(m: UnresolvedMethodCall): String = m.calleeClass.toJava + ": " + m.calleeDescriptor.toJava(m.calleeName)
}