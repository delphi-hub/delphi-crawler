package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.MappedEdge

import scala.util.Try

/*
 * This class adds information about external calls to the local database.
 *
 * It receives methods with the library they belong to marked on them, collects all the methods in a library
 * into a single map (defined in createLibraryMap), then adds these maps to a preexisting document.
 */

class ElasticCallGraphActor(client: ElasticClient) extends Actor with ActorLogging {
  override def receive: Receive = {
    case (i: MavenIdentifier, ex: Set[MappedEdge]) => {
      Try(pushEdges(i, ex)) match {
        case util.Failure(e) => {
          log.warning("Call graph pusher threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
        case util.Success(_) =>
      }
    }
  }

  def findESindex(id: MavenIdentifier) = {
    val resp = client.execute {
      search("delphi").query {
        boolQuery().must(
          termQuery("name", id.toUniqueString)
        )
      }.sourceInclude("_id")
    }.await

    if (resp.isSuccess) {
      val hits = resp.result.hits
      if (hits.total > 0) {
        hits.hits.headOption.getOrElse(throw new Exception).id
      } else {
        log.warning("WARNING: No document for mapped project {} exists! Creating new document...", id.toString)
        val resp = client.execute {
          indexInto("delphi" / "project").fields("name" -> id.toUniqueString,
            "source" -> "Maven",
            "identifier" -> Map(
              "groupId" -> id.groupId,
              "artifactId" -> id.artifactId,
              "version" -> id.version))
        }.await
        resp.result.id
      }
    } else {
      throw new Exception("Elasticsearch server cannot be reached - call graph for " + id.toString +" lost.")
    }
  }

  private def pushEdges(identifier: MavenIdentifier, edges: Set[MappedEdge]): Unit = {
    case class MappedLibrary(library: MavenIdentifier, methods: Set[String])

    def mergeEdges(edges: Set[MappedEdge]): Set[MappedLibrary] = {
      edges.headOption match {
        case None => Set[MappedLibrary]()
        case Some(frontEdge) =>
          edges.partition(_.library.equals(frontEdge.library)) match { case (hits, misses) =>
            val library = MappedLibrary(frontEdge.library, hits.map(_.method))
            mergeEdges(misses) + library
          }
      }
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
    val mergedEdges = mergeEdges(edges)
    val libraries = createLibraryMap(mergedEdges)
    client.execute {
      update(indexId).in("delphi" / "project").doc("calls" -> libraries)
    }
  }
}

object ElasticCallGraphActor{
  def props(client: ElasticClient) = Props(new ElasticCallGraphActor(client))
}
