package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.{ElasticClient, HttpClient, RequestSuccess}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

trait ArtifactIdentityQuery {
  def elasticId(identifier : MavenIdentifier)(implicit client : ElasticClient) : Option[String] = {
    client.execute {
      searchWithType(delphiProjectType) query must (
        matchQuery("name", identifier.toUniqueString)
      )
    }.await match {
      case RequestSuccess(_,_,_,SearchResponse(_, false, false, _, _, _, _, hits)) => hits.hits.headOption.map { case hit => hit.id }
      case x => None
    }
  }
}
