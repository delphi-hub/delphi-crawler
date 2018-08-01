package de.upb.cs.swt.delphi.crawler

import akka.actor.ActorSystem
import de.upb.cs.swt.delphi.crawler.instancemanagement.IRReachablePreflightCheck
import de.upb.cs.swt.delphi.crawler.storage.{ElasticIndexPreflightCheck, ElasticReachablePreflightCheck}

import scala.util.{Failure, Success, Try}

object Startup extends AppLogging {

  def showStartupInfo(implicit system : ActorSystem) = {
    log.info(s"Delphi Crawler (${BuildInfo.name} ${BuildInfo.version})")
    log.info(s"Running on Scala ${BuildInfo.scalaVersion}")
  }

  def preflightCheck(configuration: Configuration)(implicit system : ActorSystem) : Try[Configuration] = {
    log.info("Performing pre-flight checks")
    val checks = Seq(IRReachablePreflightCheck, ElasticReachablePreflightCheck, ElasticIndexPreflightCheck)

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
    log.error(s"Preflight check failed. Cause: ${e.getMessage} \n Shutting down...")
  }



}

