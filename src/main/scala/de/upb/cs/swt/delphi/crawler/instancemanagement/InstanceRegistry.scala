package de.upb.cs.swt.delphi.crawler.instancemanagement

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration, Crawler}
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.InstanceEnums.ComponentType
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.{Instance, JsonSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object InstanceRegistry extends JsonSupport with AppLogging
{
  def register(Name: String, configuration: Configuration) : Boolean = {

    val instance = Instance(None, None, Option(configuration.controlServerPort), Option(Name), Option(ComponentType.Crawler))

    implicit val system = Crawler.system
    implicit val ec = system.dispatcher
    implicit val materializer = Crawler.materializer

    Await.result(postInstance(instance, configuration.instanceRegistryUri) map {response =>
      if(response.status == StatusCodes.OK || response.status == StatusCodes.Accepted)
      {
        log.info("Successfully registered at Instance Registry.")
        true
      }
      else {
        val statuscode = response.status
        log.warning(s"Failed to register at Instance Registry, server returned $statuscode")
        false
      }

    } recover {case ex =>
      log.warning(s"Failed to register at Instance Registry, exception: $ex")
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
      Http(system).singleRequest(request)
    }

}
