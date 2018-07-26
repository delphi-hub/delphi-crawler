package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.MappedEdge

class ElasticCallGraphActor(client: HttpClient) extends Actor with ActorLogging {
  override def receive: Receive = {
    case (i: MavenIdentifier, ex: Set[MappedEdge]) => {
      pushEdges(i, ex)
    }
  }

  private def pushEdges(identifier: MavenIdentifier, edges: Set[MappedEdge]): Unit = {
    case class MappedLibrary(library: MavenIdentifier, methods: Set[String])

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
      val resp = client.execute {
        search("delphi").query {
          boolQuery().must(
            termQuery("name", id.toUniqueString)
          )
        }.sourceInclude("_id")
      }.await
      if (resp.isRight) {
        val hits = resp.right.get.result.hits
        if (hits.total > 0) {
          hits.hits.head.id
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
          resp.right.get.result.id
        }
      } else {
        throw new Exception("Elasticsearch server cannot be reached - call graph for " + id.toString +" lost.")
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
  def props(client: HttpClient) = Props(new ElasticCallGraphActor(client))
}
