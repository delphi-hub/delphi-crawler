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

import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import de.upb.cs.swt.delphi.core.model.MavenIdentifier


/**
  * Helps to determine if an artifact already exists in the elasticsearch database.
  * @author Ben Hermann
  */
trait ArtifactExistsQuery {

  import com.sksamuel.elastic4s.ElasticDsl._

  /**
    * Determines if a maven identifier was already inserted into the elasticsearch database.
    * @param identifier
    * @param client
    * @return
    */
  def exists(identifier : MavenIdentifier)(implicit client : ElasticClient) : Boolean = {
    client.execute {
      search(identifierIndexName) query must(idsQuery(identifier.toUniqueString))
    }.await match {
      case RequestSuccess(_,_,_,SearchResponse(_, false, false, _, _, _, _, hits)) => hits.hits.length > 0
      case _ => false
    }
  }
}
