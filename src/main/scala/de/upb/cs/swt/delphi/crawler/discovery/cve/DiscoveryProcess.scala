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

package de.upb.cs.swt.delphi.crawler.discovery.cve

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import de.upb.cs.swt.delphi.crawler.control.Phase
import de.upb.cs.swt.delphi.crawler.control.Phase.Phase
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

/**
  * @author Andreas Dann
  * @author Ben Hermann
  * @param configuration
  * @param elasticPool
  * @param system
  */
class DiscoveryProcess(configuration: Configuration, elasticPool: ActorRef)
                      (implicit system: ActorSystem)
  extends de.upb.cs.swt.delphi.crawler.control.Process[Long]
    with AppLogging {
  override def phase: Phase = Phase.Discovery

  override def start: Try[Long] = {
    implicit val mat: ActorMaterializer = ActorMaterializer()
    log.info("CVE Process started")
    val reader = new DataReader(Uri("https://nvd.nist.gov/feeds/json/cve/1.0/nvdcve-1.0-recent.json.zip"), system)
    val source = Source.unfoldResource[Entry, DataReader](
      () => reader,
      reader => reader.read(),
      reader => reader.close())
    source.to(Sink.foreach(c => println(c))).run()

    log.info("CVE Process terminated")
    Success(0L)
  }

  override def stop: Try[Long] = Success(0L)
}












