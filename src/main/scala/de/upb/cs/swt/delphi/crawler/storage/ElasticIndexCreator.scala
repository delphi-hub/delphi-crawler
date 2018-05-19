package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration}

import scala.util.{Success, Try}

trait ElasticIndexCreator extends AppLogging  {

  def createIndex(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.warning("Could not find Delphi index. Creating it...")
    Success(configuration)
  }
}
