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
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, MavenDownloader, MetaFile, PomFile}
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.MappedEdge
import de.upb.cs.swt.delphi.crawler.storage.ElasticCallGraphActor
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.versioning.ComparableVersion
import org.apache.maven.model.Dependency
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class CallGraphStream(configuration: Configuration) extends Actor with ActorLogging {

  override def receive: Receive = {
    case m: MavenIdentifier =>
      graphActor forward m
  }

  implicit val timeout: Timeout = 600 seconds
  val decider: Supervision.Decider = {
    case e: Exception => {log.warning("Call graph stream threw exception " + e); Supervision.Resume}
  }
  implicit val materializer: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))
  implicit val parallelism = 1
  implicit val ec = ExecutionContext.global

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  val metaReader: MetadataXpp3Reader = new MetadataXpp3Reader()
  val esClient = HttpClient(configuration.elasticsearchClientUri)

  val opalActor: ActorRef = context.actorOf(OpalActor.props(configuration))
  val mavenEdgeMapActor: ActorRef = context.actorOf(MavenEdgeMappingActor.props(configuration))
  val esEdgeSearchActor: ActorRef = context.actorOf(ElasticEdgeSearchActor.props(esClient))
  val esPushActor: ActorRef = context.actorOf(ElasticCallGraphActor.props(esClient))

  val fileGenFlow: Flow[MavenIdentifier, (PomFile, JarFile, MavenIdentifier), NotUsed] = Flow.fromFunction(fetchFiles)
  val edgeSetFlow: Flow[(JarFile, (Set[MavenIdentifier], MavenIdentifier)), ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), NotUsed] =
    Flow[(JarFile, (Set[MavenIdentifier], MavenIdentifier))].mapAsync[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier)](parallelism){
      case (jf: JarFile, (ix: Set[MavenIdentifier], i: MavenIdentifier)) => (opalActor ? jf).mapTo[Set[UnresolvedMethodCall]].map(cx => ((cx, ix), i))}
  val dependencyConverter: Flow[(PomFile, JarFile, MavenIdentifier), (Set[MavenIdentifier], (JarFile, MavenIdentifier)), NotUsed] =
    Flow.fromFunction{ case (pf, jf, id) => (mavenDependencyConverter(pf), (jf, id))}
  val filterFlow: Flow[(Set[MavenIdentifier], (JarFile, MavenIdentifier)), (JarFile, (Set[MavenIdentifier], MavenIdentifier)), NotUsed] =
    Flow.fromFunction{ case (ix, (jf, i)) => (jf, (ix, i))}

  val esEdgeMatcher: Flow[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), (((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]), MavenIdentifier), NotUsed] =
    Flow[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier)].mapAsync(parallelism){ case ((mx, ix), i) => (esEdgeSearchActor ? (mx, ix))
      .mapTo[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge])].map((_, i))}
  val mavenEdgeMatcher: Flow[(((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]), MavenIdentifier), (Set[MappedEdge], MavenIdentifier), NotUsed] =
    Flow[(((Set[UnresolvedMethodCall], Set[MavenIdentifier]), Set[MappedEdge]), MavenIdentifier)].mapAsync(parallelism){ case (((mx, ix), ex), i)
      => (mavenEdgeMapActor ? (mx, ix)).mapTo[Set[MappedEdge]].map(me => (me ++ ex, i))}

  val esPusherSink: Sink[(Set[MappedEdge], MavenIdentifier), Future[Done]] =
    Sink.foreach{ case (ex, i) => (esPushActor ! (i, ex))}

  val edgeGeneratingGraph: Flow[MavenIdentifier, ((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>

      val fileGen = b.add(fileGenFlow)
      val edgeGen = b.add(edgeSetFlow)

      fileGen.filter(t => (t._3.artifactId != null)) ~> dependencyConverter ~> filterFlow.filter{case (_, (ix, i)) =>
        if(ix.isEmpty) { log.info(i.toString + " not mapped, incomplete POM file."); false }
        else { log.info(i.toString + " mapped, valid POM file."); true }} ~> edgeGen.in

    FlowShape(fileGen.in, edgeGen.out)
  })

  val edgeMatchingGraph: Flow[((Set[UnresolvedMethodCall], Set[MavenIdentifier]), MavenIdentifier), (Set[MappedEdge], MavenIdentifier), NotUsed] =
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

  def mavenDependencyConverter(pomFile: PomFile): Set[MavenIdentifier] = {
    val pomObj = pomReader.read(pomFile.is)
    pomFile.is.close()

    def resolveProperty(str: String) = {
      if(str == null || !str.startsWith("$")) {
        str
      } else {
        val extractedVar = str.drop(2).dropRight(1)
        val splitVar = extractedVar.split("\\.", 2)
        if(splitVar(0) == "project" || splitVar(0) == "pom"){
          val evaluatedVar = splitVar(1) match {
            case "groupId" => pomObj.getGroupId
            case "artifactId" => pomObj.getArtifactId
            case "version" => pomObj.getVersion
            case _ => null
          }
          evaluatedVar
        } else {
          val evaluatedVar = pomObj.getProperties.getProperty(extractedVar)
          evaluatedVar
        }
      }
    }

    def resolveIdentifier(d: Dependency): MavenIdentifier = {
      val groupId = resolveProperty(d.getGroupId)
      val artifactId = resolveProperty(d.getArtifactId)

      if (groupId == null || artifactId == null) {
        MavenIdentifier(null, null, null, null)
      } else {
        val versionSpec = resolveProperty(d.getVersion)
        val tempId = MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, versionSpec)

        try {
          val downloader = new MavenDownloader(tempId)
          val metaFile = downloader.downloadMeta
          val metaObj = metaReader.read(metaFile.is)
          metaFile.is.close
          val versionList = metaObj.getVersioning.getVersions.asScala
          if (versionList.contains(versionSpec) || versionSpec == null) {
            tempId
          } else {
            val versionComp = new ComparableVersion(versionSpec)
            val versionNum = versionList.indexWhere(v => new ComparableVersion(v).compareTo(versionComp) >= 0)
            val version = if (versionNum == -1) versionList.last else if (versionNum == 0) versionList.head else versionList(versionNum - 1)
            MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, version)
          }
        } catch {
          case e: java.io.FileNotFoundException => {
            log.warning("Dependency {}:{} does not exist", groupId, artifactId)
            MavenIdentifier(null, null, null, null)
          }
        }
      }
    }

    val pomSet = pomObj.getDependencies()
    .asScala.toSet[Dependency].map(resolveIdentifier).filter(
      m => !(m.version == null || m.groupId == null || m.artifactId == null)
    )
    pomSet
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