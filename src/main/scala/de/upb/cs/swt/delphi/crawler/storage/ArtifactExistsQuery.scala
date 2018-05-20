package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.{HttpClient, RequestSuccess}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier

trait ArtifactExistsQuery {
  def exists(identifier : MavenIdentifier)(implicit client : HttpClient) : Boolean = {
    client.execute {
      search("delphi/mavenArtifact") query must (
        matchQuery("groupId", identifier.groupId),
        matchQuery("artifactId", identifier.artifactId),
        matchPhraseQuery("version", identifier.version)
      )
    }.await match {
      case Right(RequestSuccess(_,_,_,SearchResponse(_, false, false, _, _, _, _, hits))) => (hits.total > 0)
      case _ => false
    }
  }
}
