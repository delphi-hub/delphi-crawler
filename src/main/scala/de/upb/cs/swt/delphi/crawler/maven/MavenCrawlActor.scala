package de.upb.cs.swt.delphi.crawler.maven

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, Uri}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import coursier.core.compatibility
import coursier.{Cache, Fetch}
import coursier.maven.{MavenRepository, Pom}
import coursier.util.WebPage
import de.upb.cs.swt.delphi.crawler.BlockingHttpClient
import de.upb.cs.swt.delphi.crawler.elastic.ElasticActor.Push
import de.upb.cs.swt.delphi.crawler.maven.MavenCrawlActor.{DiscoverMetadata, ProcessMetadata, ProcessNew, StartDiscover}

import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}


/**
  * Created by benhermann on 05.02.18.
  */
class MavenCrawlActor(baseUri : Uri, elasticActor : ActorRef) extends Actor with ActorLogging {
  implicit val executionContext = context.system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http = Http(context.system)


  override def receive = {
    case StartDiscover => {
      log.info("Discovery run for baseURL {}", baseUri)

      // Poll and parse last_updated.txt
      val req = http.singleRequest(HttpRequest(method = HttpMethods.HEAD, uri = baseUri.withPath(baseUri.path + "last_updated.txt")))

      req.onComplete {
        case Success(res) => {
          log.info(res.toString())
          log.info(res.getHeader("Last-Modified").get().value())
          res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            log.info("Got response, body: " + body.utf8String)
          }
          self ! DiscoverMetadata(baseUri)
        }
        case Failure(_) => log.error("Oh no!")
      }

    }
    case DiscoverMetadata(currentUri : Uri) => {
      log.debug(s"Crawling for metadata file in: $currentUri")

      val req = http.singleRequest(HttpRequest(uri = currentUri))

      req.onComplete {
        case Success(res) => {
          res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            val files = WebPage.listFiles(currentUri.toString(), body.utf8String)
            if (files.contains("maven-metadata.xml")) {
              self ! ProcessMetadata(currentUri.withPath(currentUri.path + "maven-metadata.xml"))
            } else {
              val directories = WebPage.listDirectories(currentUri.toString(), body.utf8String)
              directories.foreach(s => self ! DiscoverMetadata(currentUri.withPath(currentUri.path + s + "/")))

            }
          }
        }
        case Failure(e) => log.warning(s"Failure for $currentUri - $e" )
      }
    }
    case ProcessMetadata(metadataUri : Uri) => {
      log.debug(s"Processing metadata at $metadataUri")

      val req = http.singleRequest(HttpRequest(uri = metadataUri))

      req.onComplete {
        case Success(res) => {
          res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            var v = for {
              xml <- \/.fromEither(coursier.core.compatibility.xmlParse(body.utf8String))
              _ <- if (xml.label == "metadata") \/-(()) else -\/("Metadata not found")
              versions <- Pom.versions(xml)
            } yield versions

            v.foreach(v => log.info(s"Found latest version: ${v.latest} for ${metadataUri}"))

          }
        }
        case Failure(_) => log.error("Metadata get failed.")
      }
    }
    case ProcessNew(m : MavenIdentifier) => {
      elasticActor ! Push(m)

      // retrieve binary
      // push to hermes
    }

      /*
      val repositories = Seq(
        MavenRepository(baseUri.toString())
      )


      val fetch = Fetch.from(repositories, Cache.fetch())



      val stuff = BlockingHttpClient.doGet(context.system, Uri(repositories.head.root0))
      var result = WebPage.listDirectories(repositories.head.root0, stuff)
      */
  }
}

object MavenCrawlActor {
  def props(baseURL : Uri, elasticActor : ActorRef) : Props = Props(new MavenCrawlActor(baseURL, elasticActor))

  case object StartDiscover
  case class DiscoverMetadata(currentUri : Uri)
  case class ProcessMetadata(metadataUri : Uri)
  case class ProcessNew(identifier : MavenIdentifier)

}
