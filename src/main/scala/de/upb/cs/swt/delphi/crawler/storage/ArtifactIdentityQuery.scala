// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ElasticClient, RequestSuccess}
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
