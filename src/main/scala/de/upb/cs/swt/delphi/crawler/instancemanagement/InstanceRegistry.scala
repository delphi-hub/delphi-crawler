package de.upb.cs.swt.delphi.crawler.instancemanagement

import de.upb.cs.swt.delphi.crawler.{Configuration, Crawler}
import de.upb.cs.swt.delphi.crawler.io.swagger.client.api.InstanceApi
import de.upb.cs.swt.delphi.crawler.io.swagger.client.core.ApiInvoker
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.InstanceEnums.ComponentType
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.{Instance, InstanceEnums}

import scala.util.{Failure, Success, Try}

object InstanceRegistry
{
  def register(Name: String, configuration: Configuration) : Try[Configuration] = {
    //TODO: Call generated API here
    val instance = Instance(None, None, Option(configuration.controlServerPort), Option(Name), Option(ComponentType.Crawler))
    val request = InstanceApi.addInstance(instance, configuration.instanceRegistryUri)
    implicit val system = Crawler.system
    ApiInvoker().execute(request)
    //Success(configuration)
    Failure(new Exception())
  }

  def retrieveElasticSearchInstance(configuration: Configuration) : Try[String] = {
    if(!configuration.usingInstanceRegistry) Failure
    //TODO: Call generated API here
    Failure(new Exception())
  }

  def sendMatchingResult(isElasticSearchReachable : Boolean, configuration: Configuration) : Try[Unit] = {
    if(!configuration.usingInstanceRegistry) Failure
    //TODO: Call generated API here
    Success()
  }
}
