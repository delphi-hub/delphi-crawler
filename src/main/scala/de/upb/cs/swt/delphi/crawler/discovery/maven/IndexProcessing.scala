package de.upb.cs.swt.delphi.crawler.discovery.maven

import java.net.URL

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import org.apache.maven.index.reader.IndexReader

import scala.util.{Failure, Success, Try}

trait IndexProcessing {

  def createSource(implicit log : LoggingAdapter) : Source[MavenIdentifier, NotUsed] = {
    val base = new URL("http://repo1.maven.org/maven2/")

    log.info("Creating source")

    val ir = Try(new MavenIndexReader(base))
    ir match {
      case Success(indexReader) => {
        Source.unfoldResource[MavenIdentifier, MavenIndexReader](
          () => indexReader,
          reader => reader.read(),
          reader => reader.close())
      }
      case Failure(e) => {
        log.error("Could not reach resource")
        Source.empty
      }
    }

  }

  class MavenIndexReader(base : URL) {

    val ir = new IndexReader(null, new HttpResourceHandler(base.toURI.resolve(".index/")))

    println("Index Reader created")
    println(ir.getIndexId)
    println(ir.getPublishedTimestamp)
    println(ir.isIncremental)
    println(ir.getChunkNames)

    lazy val cr = ir.iterator().next().iterator()

    def read() : Option[MavenIdentifier] = {
      cr.hasNext() match {
        case true => {
          println("Producing a Maven Identifier")
          val kvp = cr.next()
          val identifier = kvp.get("u").split("|".toCharArray)
          Some(MavenIdentifier(base.toString, identifier(0), identifier(1), identifier(2)))
        }
        case false => None
      }
    }
    def close() = { ir.close() }
  }
}
