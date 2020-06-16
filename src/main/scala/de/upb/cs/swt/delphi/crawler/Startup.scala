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

package de.upb.cs.swt.delphi.crawler

import akka.actor.ActorSystem
import de.upb.cs.swt.delphi.crawler.storage.{ElasticIndexPreflightCheck, ElasticReachablePreflightCheck}

import scala.util.{Failure, Success, Try}

object Startup extends AppLogging {

  def logStartupInfo(implicit system : ActorSystem) = {
    produceStartupInfo(log.info)
  }

  def printStartInfo() : Unit = {
    produceStartupInfo(println)
  }

  private def produceStartupInfo(output : String => Unit) : Unit = {
    output(s"Delphi Crawler (${BuildInfo.name} ${BuildInfo.version})")
    output(s"Running on Scala ${BuildInfo.scalaVersion} using JVM ${System.getProperty("java.version")}.")
  }

  def preflightCheck(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.info("Performing pre-flight checks")
    val checks = Seq(ElasticReachablePreflightCheck, ElasticIndexPreflightCheck)

    checks.foreach(p => {
      val result = p.check(configuration)
      result match {
        case Success(_) =>
        case Failure(e) => processPreflightError(e); return Failure(e)
      }
    })

    Success(configuration)
  }

  def processPreflightError(e : Throwable)(implicit system : ActorSystem) = {
    log.error(s"Preflight check failed. Cause: ${e} \n Shutting down...")
  }



}

