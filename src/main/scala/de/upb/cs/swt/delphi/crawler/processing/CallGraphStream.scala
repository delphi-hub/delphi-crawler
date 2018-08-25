package de.upb.cs.swt.delphi.crawler.processing


import java.io.FileNotFoundException

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.sksamuel.elastic4s.http.ElasticClient
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, MavenDownloader, PomFile}
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.MappedEdge
import de.upb.cs.swt.delphi.crawler.storage.ElasticCallGraphActor
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/*
 * This component takes a Maven identifier, determines what methods it calls in each of it's dependencies, and
 * stores each method called in each dependency in the Elasticsearch database.
 *
 * To do this, it delegates tasks to five actors, listed below. Check them for more implementation details.
 *
 * MavenDependencyActor and OpalActor find the dependencies and external calls for a project respectively,
 * ElasticEdgeSearchActor and MavenEdgeMappingActor connect them together, and ElasticCallGraphActor adds
 * all this information to the database.
 *
 * Note that to match unresolved method calls to dependencies, ElasticEdgeSearchActor is used first, and
 * MavenEdgeMappingActor is used to match any calls ElasticEdgeSearchActor missed.
 */

class CallGraphStream(configuration: Configuration) extends Actor with ActorLogging {

  override def receive: Receive = {
    case m: MavenIdentifier =>
      graphActor forward m
  }

  implicit val timeout: Timeout = 60 seconds
  val decider: Supervision.Decider = {
    case e: Exception => {log.warning("Call graph stream threw exception " + e); Supervision.Resume}
  }
  implicit val materializer: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))
  implicit val parallelism = 1
  implicit val ec = ExecutionContext.global

  val esClient = ElasticClient(configuration.elasticsearchClientUri)

  val mavenDependencyActor = context.actorOf(MavenDependencyActor.props(configuration))
  val opalActor: ActorRef = context.actorOf(OpalActor.props(configuration))
  val mavenEdgeMapActor: ActorRef = context.actorOf(MavenEdgeMappingActor.props(configuration))
  val esEdgeSearchActor: ActorRef = context.actorOf(ElasticEdgeSearchActor.props(esClient))
  val esPushActor: ActorRef = context.actorOf(ElasticCallGraphActor.props(esClient))

  val fileGenFlow: Flow[MavenIdentifier, (PomFile, JarFile, MavenIdentifier), NotUsed] = Flow.fromFunction(fetchFiles)
  val edgeSetFlow: Flow[(Set[MavenIdentifier], JarFile, MavenIdentifier), (Set[UnresolvedMethodCall], Set[MavenIdentifier], MavenIdentifier), NotUsed] =
    Flow[(Set[MavenIdentifier], JarFile, MavenIdentifier)].mapAsync(parallelism){ case (ix: Set[MavenIdentifier], jf: JarFile, i: MavenIdentifier) =>
      (opalActor ? jf).mapTo[Set[UnresolvedMethodCall]].map(cx => (cx, ix, i))}
  val dependencyConverter: Flow[(PomFile, JarFile, MavenIdentifier), (Set[MavenIdentifier], JarFile, MavenIdentifier), NotUsed] =
    Flow[(PomFile, JarFile, MavenIdentifier)].mapAsync(parallelism){ case (pf, jf, id) => (mavenDependencyActor ? pf).mapTo[Set[MavenIdentifier]].map((_, jf, id))}

  val esEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier], MavenIdentifier), (Set[UnresolvedMethodCall], Set[MavenIdentifier], Set[MappedEdge], MavenIdentifier), NotUsed] =
    Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier], MavenIdentifier)].mapAsync(parallelism){ case (mxIn, ixIn, i) => (esEdgeSearchActor ? (mxIn, ixIn))
      .mapTo[(Set[UnresolvedMethodCall], Set[MavenIdentifier], Set[MappedEdge])].map{case (mxOut, ixOut, ex) => (mxOut, ixOut, ex, i)}}
  val mavenEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier], Set[MappedEdge], MavenIdentifier), (Set[MappedEdge], MavenIdentifier), NotUsed] =
    Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier], Set[MappedEdge], MavenIdentifier)].mapAsync(parallelism){ case (mx, ix, ex, i)
      => (mavenEdgeMapActor ? (mx, ix)).mapTo[Set[MappedEdge]].map(me => (me ++ ex, i))}

  val esPusherSink: Sink[(Set[MappedEdge], MavenIdentifier), Future[Done]] =
    Sink.foreach{ case (ex, i) => (esPushActor ! (i, ex))}

  val edgeGeneratingGraph: Flow[MavenIdentifier, (Set[UnresolvedMethodCall], Set[MavenIdentifier], MavenIdentifier), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>

      val fileGen = b.add(fileGenFlow)
      val edgeGen = b.add(edgeSetFlow)

      fileGen.filter(t => (t._3.artifactId != null)) ~> dependencyConverter.filter{case (ix, jf, i) =>
        if(ix.isEmpty) { log.info(i.toString + " not mapped, incomplete POM file."); false }
        else { log.info(i.toString + " mapped, valid POM file."); true }} ~> edgeGen.in

    FlowShape(fileGen.in, edgeGen.out)
  })

  val edgeMatchingGraph: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier], MavenIdentifier), (Set[MappedEdge], MavenIdentifier), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      val esMatcher = b.add(esEdgeMatcher)
      val mvMatcher = b.add(mavenEdgeMatcher)

      esMatcher ~> mvMatcher

      FlowShape(esMatcher.in, mvMatcher.out)
  })

  val edgeMappingGraph: Sink[MavenIdentifier, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit b =>
    val edgeGenerator = b.add(edgeGeneratingGraph)

    edgeGenerator ~> edgeMatchingGraph ~> esPusherSink

    SinkShape(edgeGenerator.in)
  })

  def fetchFiles(mavenIdentifier: MavenIdentifier): (PomFile, JarFile, MavenIdentifier) = {
    try {
      val downloader = new MavenDownloader(mavenIdentifier)
      (downloader.downloadPom(), downloader.downloadJar(), mavenIdentifier)
    } catch {
      case e: FileNotFoundException => {
        log.info("{} not mapped, missing POM file", mavenIdentifier.toString)
        (PomFile(null), JarFile(null, null), MavenIdentifier(null, null, null, null))  //There might be a more elegant way of doing this
      }
    }
  }

  val actorSource: Source[MavenIdentifier, ActorRef] = Source.actorRef(5000, OverflowStrategy.dropNew)  //We may need to adjust this

  val callGraphGraph: RunnableGraph[ActorRef] = actorSource.toMat(edgeMappingGraph)(Keep.left)

  val graphActor: ActorRef = callGraphGraph.run()
}

object CallGraphStream{
  def props(configuration: Configuration) = Props(new CallGraphStream(configuration))

  case class MappedEdge(library: MavenIdentifier, method: String)   //I'm not sure if this is the best place to put these
  def unresMCtoStr(m: UnresolvedMethodCall): String = m.calleeClass.toJava + ": " + m.calleeDescriptor.toJava(m.calleeName)
}