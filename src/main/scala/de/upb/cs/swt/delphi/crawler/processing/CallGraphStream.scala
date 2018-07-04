package de.upb.cs.swt.delphi.crawler.processing

import java.io.File
import java.net.URL

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, Unzip, Zip, ZipWith}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.util.Timeout
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, MavenDownloader, PomFile}
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.maven.model.Dependency
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.opalj.ai.analyses.cg.UnresolvedMethodCall
import org.opalj.br.analyses.Project

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

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  val esClient = HttpClient(configuration.elasticsearchClientUri)
  val opalActor: ActorRef = context.actorOf(OpalActor.props(configuration))

  val fileGenFlow: Flow[MavenIdentifier, (JarFile, PomFile), NotUsed] = Flow.fromFunction(fetchFiles)
  val edgeSetFlow: Flow[JarFile, Set[UnresolvedMethodCall], NotUsed] = Flow[JarFile]
    .mapAsync[Set[UnresolvedMethodCall]](1)((jf: JarFile) => (opalActor ? jf).mapTo[Set[UnresolvedMethodCall]])
  val dependencyConverter: Flow[PomFile, Set[MavenIdentifier], NotUsed] = Flow.fromFunction(mavenDependencyConverter)

  val esEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]), NotUsed] =
    Flow.fromFunction(t => (t, Set()))  //TODO
  val mavenEdgeMatcher: Flow[(Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge], NotUsed] = Flow.fromFunction(matchNewEdges)

  val esPusherSink: Sink[(MavenIdentifier, Set[MappedEdge]), Future[Done]] = Sink
    .foreach[(MavenIdentifier, Set[MappedEdge])]{ case (i, e) => pushEdges(i, e)}

  val edgeGeneratingGraph: Flow[MavenIdentifier, (Set[UnresolvedMethodCall], Set[MavenIdentifier]), NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    val unzipper = b.add(Unzip[JarFile, PomFile])
    val zipper = b.add(Zip[Set[UnresolvedMethodCall], Set[MavenIdentifier]])
    val fileGen = b.add(fileGenFlow)

    fileGen ~> unzipper.in
    unzipper.out0 ~> edgeSetFlow ~> zipper.in0
    unzipper.out1 ~> dependencyConverter ~> zipper.in1

    FlowShape(fileGen.in, zipper.out)
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
    val broadcaster = b.add(Broadcast[MavenIdentifier](2))
    val zipper = b.add(Zip[MavenIdentifier, Set[MappedEdge]])

    broadcaster.out(0) ~> zipper.in0
    broadcaster.out(1) ~> edgeGeneratingGraph ~> edgeMatchingGraph ~> zipper.in1
    zipper.out ~> esPusherSink

    SinkShape(broadcaster.in)
  })

  def fetchFiles(mavenIdentifier: MavenIdentifier): (JarFile, PomFile) = {
    log.info("Fetching files for " + mavenIdentifier.toString)
    val downloader = new MavenDownloader(mavenIdentifier)
    (downloader.downloadJar(), downloader.downloadPom())
  }

  def mavenDependencyConverter(pomFile: PomFile): Set[MavenIdentifier] = {
    pomFile.is.mark(5000)
    val pomSet = pomReader.read(pomFile.is).getDependencies()
    .asScala.toSet[Dependency].map(d =>
      MavenIdentifier(
        configuration.mavenRepoBase.toString(),
        d.getGroupId(),
        d.getArtifactId(),
        d.getVersion()
      ))
    pomFile.is.reset()
    log.info("Contents of POM file:\n" + IOUtils.toString(pomFile.is))
    pomFile.is.close()
    pomSet
  }

  //Might want to move this into an actor, and see if there is a faster way to determine if a method exists within a project
  def matchNewEdges(edgeTuple: (Set[UnresolvedMethodCall], Set[MavenIdentifier])): Set[MappedEdge] = {

    def edgeSearch(edgeSet: Set[UnresolvedMethodCall], mavenList: List[MavenIdentifier]): Set[MappedEdge] = {
      if (edgeSet.isEmpty){
        Set[MappedEdge]()
      } else {
        if (mavenList.isEmpty) {
          log.warning("ERROR: The following virtual methods could not be mapped to any library:")
          edgeSet.foreach(m => log.warning(m.calleeClass.toJava + ": " + m.calleeDescriptor.toJava(m.calleeName)))
          Set[MappedEdge]()
        } else {
          val identifier: MavenIdentifier = mavenList.head
          val library: Project[URL] = loadProject(identifier)
          val splitSet = edgeSet.partition(m => library.resolveMethodReference(m.calleeClass, m.calleeName, m.calleeDescriptor).isDefined)
          val mappedEdges = splitSet._1.map(m => MappedEdge(identifier, m.calleeClass.toJava + ": " + m.calleeDescriptor.toJava(m.calleeName)))
          mappedEdges ++ edgeSearch(splitSet._2, mavenList.tail)
        }
      }
    }

    def loadProject(identifier: MavenIdentifier): Project[URL] = {
      val jarFile = new MavenDownloader(identifier).downloadJar()
      val dummyFile = new File(configuration.tempFileStorage + identifier.toString + ".jar")
      FileUtils.copyInputStreamToFile(jarFile.is, dummyFile)
      val project = Project(dummyFile)

      dummyFile.delete()
      project
    }

    val edgeSet = edgeTuple._1; val mavenList = edgeTuple._2.toList
    edgeSearch(edgeSet, mavenList)
  }

  def pushEdges(identifier: MavenIdentifier, edges: Set[MappedEdge]): Unit = {
    case class MappedLibrary(library: MavenIdentifier, methods: Set[String])
    case class IDclass(_id: String)

    def mergeEdges(edges: Set[MappedEdge]): Set[MappedLibrary] = {
      if (edges.isEmpty) {
        Set[MappedLibrary]()
      } else {
        val splitSet = edges.partition(_.library.equals(edges.head.library))
        val library = MappedLibrary(edges.head.library, splitSet._1.map(_.method))
        mergeEdges(splitSet._2) + library
      }
    }

    def findESindex(id: MavenIdentifier) = {
      val test = esClient.execute {
        search("delphi").termQuery("identifier.groupId", id.groupId).termQuery("identifier.artifactId", id.artifactId)
          .termQuery("identifier.version", id.version).sourceInclude("_id")
      }.await
      val hits = test.right.get.result.hits.hits
      hits.head.id
    }

    def createLibraryMap(set: Set[MappedLibrary]) = {
      set.map(l => Map(
        "name" -> l.library.toString,
        "identifier" -> Map(
          "groupId" -> l.library.groupId,
          "artifactId" -> l.library.artifactId,
          "version" -> l.library.version
        ),
        "methods" -> l.methods.toSeq
      )).toSeq
    }

    val indexId = findESindex(identifier)
    val libraries = createLibraryMap(mergeEdges(edges))
    esClient.execute {
      update(indexId).in("delphi" / "project").doc("calls" -> libraries)
    }
  }

  val actorSource: Source[MavenIdentifier, ActorRef] = Source.actorRef(1000, OverflowStrategy.dropNew)  //We may need to adjust this

  val printSink: Sink[Set[MappedEdge], Future[Done]] = Sink.foreach[Set[MappedEdge]] { ex =>
    log.info("The following methods were found:")
    ex.foreach{
      e => log.info("In " + e.library.toString + ": " + e.method)
    }
  }

  val callGraphGraph: RunnableGraph[ActorRef] = actorSource.toMat(edgeMappingGraph)(Keep.left)

  val graphActor: ActorRef = callGraphGraph.run()

  case class MappedEdge(library: MavenIdentifier, method: String)
}

object CallGraphStream{
  def props(configuration: Configuration) = Props(new CallGraphStream(configuration))
}