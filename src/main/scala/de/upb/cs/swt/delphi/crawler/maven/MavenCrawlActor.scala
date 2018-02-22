package de.upb.cs.swt.delphi.crawler.maven

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, ZipWith}
import coursier.core.compatibility
import coursier.shaded.org.jsoup.Jsoup
import de.upb.cs.swt.delphi.crawler.elastic.ElasticActor.Push
import de.upb.cs.swt.delphi.crawler.maven.MavenCrawlActor.{DiscoverMetadata, ProcessMetadata, ProcessNew, StartDiscover}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * Created by benhermann on 05.02.18.
  */
class MavenCrawlActor(baseUri: Uri, elasticActor: ActorRef) extends Actor with ActorLogging {
  implicit val executionContext = context.system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val system = context.system
  val http = Http(context.system)

  val uriToRequest = Flow[Uri].map { currentUri => HttpRequest(uri = currentUri) }


  override def receive = {
    case StartDiscover => {
      log.info("Discovery run for baseURL {}", baseUri)

      // Poll and parse last_updated.txt
      val req = http.singleRequest(HttpRequest(method = HttpMethods.HEAD, uri = baseUri.withPath(baseUri.path + "last_updated.txt")))

      req.onComplete {
        case Success(res) => {
          log.info(res.toString())
          log.info(res.getHeader("Last-Modified").get().value())

          // TODO check last modified
          self ! DiscoverMetadata(baseUri)

        }
        case Failure(_) => log.error("Oh no!")
      }

    }
    case DiscoverMetadata(currentUri : Uri) => {
      val host = currentUri.authority.host.toString()
      val port = currentUri.effectivePort

      /**
        * Represents a flow that can connect to a specific host and host.
        * It can be used to construct outgoing HTTP connections
        */
      val conFlow = http.outgoingConnection(host, port)

      /**
        * Represents a parsing flow from a HttpResponse to a sequence of strings containing the links of the website requested.
        * Generally useful for directory traversal.
        * It excludes the parent directory and current directory link.
        */
      val parseFlow = Flow[(HttpRequest,HttpResponse)].flatMapConcat {
        case c @ (_, HttpResponse(StatusCodes.OK, _, _, _)) =>
          c._2.entity.dataBytes
            .map(bs => { bs.utf8String })
            .map(s => Jsoup.parse(s).select("a[href]").asScala.map(e => e.attr("href")))
            .mapConcat(_.to[scala.collection.immutable.Iterable])
            .filter(s => !s.equals("../") && !s.equals("./"))
            .map(s => c._1.uri.withPath(c._1.uri.path + s).toString())
      }


      def identityWithLog(label : String) = Flow[String].map(s => { log.info(label + s"$s"); s })

      /**
        * The grabDirectory graph takes a stream of strings, checks if the end with a /,
        * converts them to Uri objects, constructs a HttpRequest, executes this request,
        * and combines request and response in a tuple.
        * This tuple is then parsed to a list of strings containing the links of this page.
        *
        * checkForDirectory -> convertToUri -> constructRequest -> connect -> combine -> parse
        *                                             |                         /\
        *                                             |                         |
        *                                             ---------------------------
        */
      val grabDirectory = GraphDSL.create () {
        implicit b =>
          import GraphDSL.Implicits._

          val bcast = b.add(Broadcast[HttpRequest](2))
          val parser = b.add(parseFlow)
          val conn = b.add(conFlow)

          val constructRequest = b.add(uriToRequest)
          val toUri = b.add(Flow[String].map(Uri(_)))
          val directoryGate = b.add(Flow[String].filter(s => s.endsWith("/")))

          val tupleMerge = b.add(ZipWith[HttpRequest, HttpResponse,(HttpRequest, HttpResponse)]((_,_)))

          directoryGate ~> toUri ~> constructRequest ~> bcast.in
          bcast.out(0) ~> conn ~> tupleMerge.in1
          bcast.out(1) ~> tupleMerge.in0

          tupleMerge.out ~> parser

          FlowShape(directoryGate.in,parser.out)
      }

      def websiteLinkSource (uris : Seq[Uri]) : Source[String, NotUsed] = {
        Source.fromIterator{ () => uris.map(_.toString()).toIterator }
          .via(Flow.fromGraph(grabDirectory))
      }


      var workList : Seq[Uri] = Seq(currentUri)
      val mavenMetadata : ArrayBuffer[Uri] = ArrayBuffer()

      // This is a bit ugly considering the really nice stream definitions above, but hey... it should work.
      while (!workList.isEmpty) {
        log.info(s"Started working on a worklist of size: ${workList.size}")
        var result = websiteLinkSource(workList).runWith(Sink.seq)
        Await.result(result, Duration.Inf)

        val foundUrls = result.value.get.get
        log.info(s"Found ${foundUrls.size} new urls to work on.")
        mavenMetadata ++= foundUrls.filter(_.endsWith("maven-metadata.xml")).map(Uri(_))
        workList = foundUrls.filter(_.endsWith("/")).map(Uri(_))
      }

      log.info(s"Found ${mavenMetadata.size} metadata files")
      mavenMetadata.foreach(s => log.info(s.toString()))

      self ! ProcessMetadata(mavenMetadata)

    }
    case ProcessMetadata(metadataUris: Seq[Uri]) => {
      // Read metadata file
      // Determine versions currently not known
      // push them to the ProcessNew process
      if (metadataUris.nonEmpty) {

        val parseFlow = Flow[HttpResponse].flatMapConcat {
          case c @ HttpResponse(StatusCodes.OK, _, _, _) =>
            c.entity.dataBytes
              .map(bs => { bs.utf8String })
              .map(s => compatibility.xmlParse(s))
              .map(x => x.right.get)

        }

        val connection = http.outgoingConnection(metadataUris.head.authority.host.toString(), metadataUris.head.effectivePort)
        Source.fromIterator(() => metadataUris.toIterator)
          .via(uriToRequest)
          .via(connection)
          .via(parseFlow)
          .to(Sink.foreach(s => println(s)))
          .run()


      }
    }
    case ProcessNew(m: MavenIdentifier) => {
      elasticActor ! Push(m)

      // retrieve binary
      // push to hermes
    }

  }
}





object MavenCrawlActor {
  def props(baseURL: Uri, elasticActor: ActorRef): Props = Props(new MavenCrawlActor(baseURL, elasticActor))

  case object StartDiscover

  case class DiscoverMetadata(currentUri: Uri)

  case class ProcessMetadata(metadataUris: Seq[Uri])

  case class ProcessNew(identifier: MavenIdentifier)

}
