package de.upb.cs.swt.delphi.crawler.instancemanagement

import de.upb.cs.swt.delphi.crawler.Configuration

import scala.util.{Failure, Success, Try}

object InstanceRegistry
{
  def register(Name: String, configuration: Configuration) : Try[Configuration] = {
    //TODO: Call generated API here
    Success(configuration)
  }

  def retrieveElasticSearchInstance(configuration: Configuration) : Try[String] = {
    if(!configuration.usingInstanceRegistry) Failure
    //TODO: Call generated API here
    Success("")
  }

  def sendMatchingResult(isElasticSearchReachable : Boolean, configuration: Configuration) : Try[Unit] = {
    if(!configuration.usingInstanceRegistry) Failure
    //TODO: Call generated API here
    Success()
  }
}
