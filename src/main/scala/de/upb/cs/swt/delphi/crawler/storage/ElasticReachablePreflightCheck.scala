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
import com.sksamuel.elastic4s.ElasticApi.nodeInfo
import com.sksamuel.elastic4s.ElasticDsl.NodeInfoHandler
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceRegistry
import de.upb.cs.swt.delphi.crawler.tools.ElasticHelper
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

/**
  * A pre-flight check if elasticsearch is available or not
  *
  * @author Ben Hermann
  */
object ElasticReachablePreflightCheck extends PreflightCheck {
  /**
    * Takes the provided configuration and tried to access an elasticsearch server.
    *
    * @param configuration The configuration to access the elasticsearch server
    * @param system The currently active actor system to bind the execution context
    * @return
    */
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    implicit val ec : ExecutionContext = system.dispatcher
    lazy val client = ElasticHelper.buildElasticClient(configuration)

    val f = (client.execute {
      nodeInfo()
    } map { i => {
      InstanceRegistry.sendMatchingResult(isElasticSearchReachable = true, configuration)
      Success(configuration)
    }
    } recover { case e =>
      InstanceRegistry.sendMatchingResult(isElasticSearchReachable = false, configuration)
      Failure(e)

    }).andThen {
      case _ => client.close()
    }

    Await.result(f, Duration.Inf)
  }
}
