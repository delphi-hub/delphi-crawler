package de.upb.cs.swt.delphi.crawler.instancemanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import de.upb.cs.swt.delphi.crawler.{Configuration, Crawler}
import de.upb.cs.swt.delphi.crawler.io.swagger.client.api.InstanceApi
import de.upb.cs.swt.delphi.crawler.io.swagger.client.core.ApiInvoker
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.InstanceEnums.ComponentType
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.{Instance, InstanceEnums, JsonSupport}
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object InstanceRegistry extends JsonSupport
{
  def register(Name: String, configuration: Configuration) : Boolean = {

    val instance = Instance(None, None, Option(configuration.controlServerPort), Option(Name), Option(ComponentType.Crawler))

    implicit val system = Crawler.system
    implicit val ec = system.dispatcher
    implicit val materializer = Crawler.materializer

    Await.result(postInstance(instance, configuration.instanceRegistryUri) map {response =>
      if(response.status == StatusCodes.OK || response.status == StatusCodes.Accepted)
      {
        println("Successfully registered at Instance Registry.")
        true
      }
      else {
        val statuscode = response.status
        println(s"Failed to register at Instance Registry, server returned $statuscode")
        false
      }

    } recover {case ex =>
      println(s"Failed to register at Instance Registry, exception: $ex")
      false
    }, Duration.Inf)
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

  def postInstance(instance : Instance, uri: String) (implicit system: ActorSystem, ec : ExecutionContext) : Future[HttpResponse] =
    Marshal(instance).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
      println("*****REQUEST: "+ request.toString())
      Http(system).singleRequest(request)
    }

}
