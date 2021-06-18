package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.MultiSearchResponse
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.{MappedEdge, unresMCtoStr}

import org.opalj.br.DeclaredMethod

import scala.util.Try

/*
 * This class matches unresolved method calls to dependencies using the local Elasticsearch server.
 *
 * All method call/dependency pairs are searched for in the database to see if they exist. If a pair does exist, that
 * method is marked as belonging to that dependency.
 */

class ElasticEdgeSearchActor(client: ElasticClient) extends Actor with ActorLogging{

  val maxBatchSize = 100

  override def receive: Receive = {
    case (mx: Set[DeclaredMethod], ix: Set[MavenIdentifier]) => {
      Try(segmentFun(searchMethods, maxBatchSize)(mx, ix.toList)) match {
        case util.Success((unmapped, mapped)) =>
          sender() ! (unmapped, ix, mapped)
        case util.Failure(e) => {
          log.warning("Elastic mapper threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
    }
  }

  //Splits the set of methods in "batch" sized chucks before passing them to the search function, to prevent
  //  the construction of a search too large to be run
  private def segmentFun(fx: (Set[DeclaredMethod], List[MavenIdentifier]) => (Set[DeclaredMethod], Set[MappedEdge]), batch: Int)
                     (mx: Set[DeclaredMethod], ix: List[MavenIdentifier]): (Set[DeclaredMethod], Set[MappedEdge]) = {
    if (mx.size > batch) {
      mx.splitAt(batch) match { case (currSeg, restSeg) =>
        val segmentResults = fx (currSeg, ix)
        val remainingResults = segmentFun (fx, batch) (restSeg, ix)
        (segmentResults, remainingResults) match { case ((seg1, seg2), (rem1, rem2)) =>
          (seg1 ++ rem1, seg2 ++ rem2)
        }
      }
    } else {
      fx(mx, ix)
    }
  }

  def genSearchDef(call: DeclaredMethod, id: MavenIdentifier) = {
    search("delphi").query {
      nestedQuery("calls",
        boolQuery().must(
          termQuery("calls.name", id.toString),
          termQuery("calls.methods", unresMCtoStr(call))
        )
      )
    }
  }

  def searchEsDb(calls: List[DeclaredMethod], id: MavenIdentifier): DeclaredMethod => Boolean = {
    val resp: Response[MultiSearchResponse] = client.execute{
      multi(
        for(call <- calls) yield genSearchDef(call, id)
      )
    }.await

    val result = resp.result.items
    val hitIndices = result.filter(_.response.isRight).filter(_.response.right.getOrElse(throw new Exception).totalHits > 0).map(_.index)
    val hits = for (i <- hitIndices) yield calls(i)

    hits.contains
  }

  private def searchMethods(calls: Set[DeclaredMethod], ids: List[MavenIdentifier]): (Set[DeclaredMethod], Set[MappedEdge]) = {
    if (calls.isEmpty) (Set[DeclaredMethod](), Set[MappedEdge]())

    ids.headOption match {
      case None => (calls, Set[MappedEdge]())
      case Some(id) => {
        def exists = searchEsDb(calls.toList, id)
        calls.partition(exists) match {
          case (hits, misses) => {
            val mappedMethods: Set[MappedEdge] = hits.map(m => MappedEdge(id, unresMCtoStr(m)))

            searchMethods(misses, ids.tail) match {
              case (unresolved, mapped) =>
                (unresolved, mapped ++ mappedMethods)
            }
          }
        }
      }
    }
  }
}

object ElasticEdgeSearchActor {
  def props(client: ElasticClient) = Props(new ElasticEdgeSearchActor(client))
}
