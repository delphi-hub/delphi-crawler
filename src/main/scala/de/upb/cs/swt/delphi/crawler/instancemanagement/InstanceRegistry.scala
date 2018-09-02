package de.upb.cs.swt.delphi.crawler.instancemanagement

import java.net.InetAddress

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration, Crawler}
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.InstanceEnums.ComponentType
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.{Instance, JsonSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object InstanceRegistry extends JsonSupport with AppLogging
{

  implicit val system = Crawler.system
  implicit val ec = system.dispatcher
  implicit val materializer = Crawler.materializer

  def register(configuration: Configuration) : Boolean = {

    val instance = createInstance(None,configuration.controlServerPort, configuration.instanceName)

    Await.result(postInstance(instance, configuration.instanceRegistryUri + "/register") map {response =>
      if(response.status == StatusCodes.OK){
        Await.result(Unmarshal(response.entity).to[String] map { assignedID =>
          val id = assignedID.toLong
          log.info(s"Successfully registered at Instance Registry, got ID $id.")
          true
        } recover { case ex =>
          log.warning(s"Failed to read assigned ID from Instance Registry, exception: $ex")
          false
        }, Duration.Inf)
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

  def retrieveElasticSearchInstance(configuration: Configuration) : Try[Instance] = {
    if(!configuration.usingInstanceRegistry) Failure(new RuntimeException("Cannot get ElasticSearch instance from Instance Registry, no Instance Registry available."))
    else {
      val request = HttpRequest(method = HttpMethods.GET, configuration.instanceRegistryUri + "/matchingInstance?ComponentType=ElasticSearch")

      Await.result(Http(system).singleRequest(request) map {response =>
        val status = response.status
        if(status == StatusCodes.OK) {

          Await.result(Unmarshal(response.entity).to[Instance] map {instance =>
            val elasticIP = instance.iP
            log.info(s"Instance Registry assigned ElasticSearch instance at ${elasticIP.getOrElse("None")}")
            Success(instance)
          } recover {case ex =>
            log.warning(s"Failed to read response from Instance Registry, exception: $ex")
            Failure(ex)
          }, Duration.Inf)
        }
        else{
          log.warning(s"Failed to read response from Instance Registry, server returned $status")
          Failure(new RuntimeException(s"Failed to read response from Instance Registry, server returned $status"))
        }
      } recover { case ex =>
        log.warning(s"Failed to request ElasticSearch instance from Instance Registry, exception: $ex ")
        Failure(ex)
      }, Duration.Inf)
    }
  }

  def sendMatchingResult(isElasticSearchReachable : Boolean, configuration: Configuration) : Try[Unit] = {
    if(!configuration.usingInstanceRegistry) Failure(new RuntimeException("Cannot post matching result to Instance Registry, no Instance Registry available."))
    if(configuration.elasticsearchInstance.iD.isEmpty) Failure(new RuntimeException("Cannot post matching result to Instance Registry, assigned ElasticSearch instance has no ID."))

    val IdToPost = configuration.elasticsearchInstance.iD.get
    val request = HttpRequest(method = HttpMethods.POST, configuration.instanceRegistryUri + s"/matchingResult?Id=$IdToPost&MatchingSuccessful=$isElasticSearchReachable")

    Await.result(Http(system).singleRequest(request) map {response =>
      if(response.status == StatusCodes.OK){
        log.info("Successfully posted matching result to Instance Registry.")
        Success()
      }
      else {
        val statuscode = response.status
        log.warning(s"Failed to post matching result to Instance Registry, server returned $statuscode")
        Failure(new RuntimeException(s"Failed to post matching result to Instance Registry, server returned $statuscode"))
      }

    } recover {case ex =>
      log.warning(s"Failed to post matching result to Instance Registry, exception: $ex")
      Failure(new RuntimeException(s"Failed to post matching result tot Instance Registry, exception: $ex"))
    }, Duration.Inf)
  }

  def postInstance(instance : Instance, uri: String) () : Future[HttpResponse] =
    Marshal(instance).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
      Http(system).singleRequest(request)
    }


  private def createInstance(id: Option[Long], controlPort : Int, name : String) : Instance = Instance(id, Option(InetAddress.getLocalHost().getHostAddress()), Option(controlPort), Option(name), Option(ComponentType.Crawler))
}
