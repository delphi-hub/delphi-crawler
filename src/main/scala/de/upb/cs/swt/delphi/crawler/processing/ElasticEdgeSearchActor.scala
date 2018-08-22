package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.{MappedEdge, unresMCtoStr}
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

/*
 * This class matches unresolved method calls to dependencies using the local Elasticsearch server.
 *
 * All method call/dependency pairs are searched for in the database to see if they exist. If a pair does exist, that
 * method is marked as belonging to that dependency.
 */

class ElasticEdgeSearchActor(client: HttpClient) extends Actor with ActorLogging{

  val maxBatchSize = 150

  override def receive: Receive = {
    case (mx: Set[UnresolvedMethodCall], ix: Set[MavenIdentifier]) => {
      try {
        segmentFun(searchMethods, maxBatchSize)(mx, ix.toList) match {
          case (unmapped, mapped) =>
            sender() ! (unmapped, ix, mapped)
        }
      } catch {
        case e: Exception =>
          log.warning("Elastic mapper threw exception " + e)
          akka.actor.Status.Failure(e)
      }
    }
  }

  //Splits the set of methods in "batch" sized chucks before passing them to the search function, to prevent
  //  the construction of a search too large to be run
  private def segmentFun(fx: ((Set[UnresolvedMethodCall], List[MavenIdentifier]) => (Set[UnresolvedMethodCall], Set[MappedEdge])), batch: Int)
                     (mx: Set[UnresolvedMethodCall], ix: List[MavenIdentifier]): (Set[UnresolvedMethodCall], Set[MappedEdge]) = {
    if (mx.size > batch) {
      val segmentedMx = mx.splitAt(batch)
      val segmentResults = fx(segmentedMx._1, ix)
      val remainingResults = segmentFun(fx, batch)(segmentedMx._2, ix)
      (segmentResults._1 ++ remainingResults._1, segmentResults._2 ++ remainingResults._2)
    } else {
      fx(mx, ix)
    }
  }

  private def searchMethods(calls: Set[UnresolvedMethodCall], ids: List[MavenIdentifier]): (Set[UnresolvedMethodCall], Set[MappedEdge]) = {

    def genSearchDef(call: UnresolvedMethodCall, id: MavenIdentifier) = {
      search("delphi").query {
        nestedQuery("calls",
          boolQuery().must(
            termQuery("calls.name", id.toString),
            termQuery("calls.methods", unresMCtoStr(call))
          )
        )
      }
    }

    def searchEsDb(calls: List[UnresolvedMethodCall], id: MavenIdentifier): (UnresolvedMethodCall) => Boolean = {
      val resp = client.execute{
        multi(
          for(call <- calls) yield genSearchDef(call, id)
        )
      }.await

      val result = resp.right.get.result.items
      val hitIndices = result.filter(_.response.isRight).filter(_.response.right.get.totalHits > 0).map(_.index)
      val hits = for (i <- hitIndices) yield calls(i)

      hits.contains
    }

    if (ids.isEmpty) (calls, Set[MappedEdge]())
    else if (calls.isEmpty) (Set[UnresolvedMethodCall](), Set[MappedEdge]())
    else {
      val id = ids.head
      def exists = searchEsDb(calls.toList, id)
      val splitSet = calls.partition(exists)
      val mappedMethods: Set[MappedEdge] = splitSet._1.map(m => MappedEdge(id, unresMCtoStr(m)))

      searchMethods(splitSet._2, ids.tail) match { case (unresolved, mapped) =>
        (unresolved, mapped ++ mappedMethods)
      }
    }
  }
}

object ElasticEdgeSearchActor {
  def props(client: HttpClient) = Props(new ElasticEdgeSearchActor(client))
}
