package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.{HttpClient, RequestSuccess}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

trait ArtifactExistsQuery {
  def exists(identifier : MavenIdentifier)(implicit client : HttpClient) : Boolean = {
    client.execute {
      searchWithType(delphiProjectType) query must (
        matchQuery("name", identifier.toUniqueString)
      )
    }.await match {
      case Right(RequestSuccess(_,_,_,SearchResponse(_, false, false, _, _, _, _, hits))) => (hits.total > 0)
      case x => println(x); false
    }
  }
}
