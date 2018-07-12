package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.processing.CallGraphStream.{MappedEdge, unresMCtoStr}
import org.opalj.ai.analyses.cg.UnresolvedMethodCall

class ElasticEdgeSearchActor(client: HttpClient) extends Actor with ActorLogging{

  override def receive: Receive = {
    case (mx: Set[UnresolvedMethodCall], ix: Set[MavenIdentifier]) => {
      searchMethods(mx, ix.toList) match { case (unmapped, mapped) =>
        sender() ! ((unmapped, ix), mapped)
      }
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
