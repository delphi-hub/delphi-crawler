package de.upb.cs.swt.delphi.crawler

import akka.actor.ActorSystem
import de.upb.cs.swt.delphi.crawler.storage.ElasticPreflightCheck

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object Startup extends AppLogging {

  def showStartupInfo(implicit system : ActorSystem) = {
    log.info(s"Delphi Crawler (${BuildInfo.name} ${BuildInfo.version})")
    log.info(s"Running on Scala ${BuildInfo.scalaVersion}")
  }

  def preflightCheck(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.info("Performing pre-flight checks")
    implicit val context : ExecutionContext = system.dispatcher
    val result = ElasticPreflightCheck.check(configuration)
    result match {
      case Success(configuration) => println(configuration)
      case Failure(e) => processPreflightError(e)
    }
    result
  }

  def processPreflightError(e : Throwable)(implicit system : ActorSystem) = {
    log.error(s"Preflight check failed. Cause: ${e.getMessage} \n Shutting down...")
  }



}

