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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.sksamuel.elastic4s.ElasticApi.indexExists
import com.sksamuel.elastic4s.ElasticDsl.IndexExistsHandler
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess}
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceRegistry
import de.upb.cs.swt.delphi.crawler.tools.ElasticHelper
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object ElasticIndexPreflightCheck extends PreflightCheck with ElasticIndexMaintenance {
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    implicit val ec : ExecutionContext = system.dispatcher
    lazy val client = ElasticHelper.buildElasticClient(configuration)

    val f = client.execute {
      indexExists(identifierIndexName) //TODO: May want to check all three indices
    } andThen {
      case _ => client.close()
    }
    val delphiIndexExists = Await.result(f, Duration.Inf)

    delphiIndexExists match {
      case RequestSuccess(StatusCodes.NotFound.intValue, _, _, _) => createDelphiIndex(configuration)
      case RequestSuccess(_,_,_,_) => {
        isIndexCurrent(configuration) match {
          case true => Success(configuration) // This is fine
          case false => migrateIndex(configuration) // This needs some work
        }
      }
      case RequestFailure(_, _, _, e) =>  {
        InstanceRegistry.sendMatchingResult(false, configuration)
        Failure(new ElasticException(e))
      }
    }
  }
}
