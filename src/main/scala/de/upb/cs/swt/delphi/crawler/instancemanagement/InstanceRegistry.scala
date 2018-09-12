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

package de.upb.cs.swt.delphi.crawler.instancemanagement

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import de.upb.cs.swt.delphi.crawler.{AppLogging, Configuration, Crawler}
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.InstanceEnums.ComponentType
import de.upb.cs.swt.delphi.crawler.io.swagger.client.model.{Instance, JsonSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object InstanceRegistry extends JsonSupport with AppLogging
{

  implicit val system : ActorSystem = Crawler.system
  implicit val ec  : ExecutionContext = system.dispatcher
  implicit val materializer : ActorMaterializer = Crawler.materializer


  def register(configuration: Configuration) : Try[Long] = {
    val instance = createInstance(None,configuration.controlServerPort, configuration.instanceName)

    Await.result(postInstance(instance, configuration.instanceRegistryUri + "/register") map {response =>
      if(response.status == StatusCodes.OK){
        Await.result(Unmarshal(response.entity).to[String] map { assignedID =>
          val id = assignedID.toLong
          log.info(s"Successfully registered at Instance Registry, got ID $id.")
          Success(id)
        } recover { case ex =>
          log.warning(s"Failed to read assigned ID from Instance Registry, exception: $ex")
          Failure(ex)
        }, Duration.Inf)
      }
      else {
        val statuscode = response.status
        log.warning(s"Failed to register at Instance Registry, server returned $statuscode")
        Failure(new RuntimeException(s"Failed to register at Instance Registry, server returned $statuscode"))
      }

    } recover {case ex =>
      log.warning(s"Failed to register at Instance Registry, exception: $ex")
      Failure(ex)
    }, Duration.Inf)
  }

  def retrieveElasticSearchInstance(configuration: Configuration) : Try[Instance] = {
    if(!configuration.usingInstanceRegistry) {
      Failure(new RuntimeException("Cannot get ElasticSearch instance from Instance Registry, no Instance Registry available."))
    } else {
      val request = HttpRequest(method = HttpMethods.GET, configuration.instanceRegistryUri + "/matchingInstance?ComponentType=ElasticSearch")

      Await.result(Http(system).singleRequest(request) map {response =>
        val status = response.status
        if(status == StatusCodes.OK) {

          Await.result(Unmarshal(response.entity).to[Instance] map {instance =>
            val elasticIP = instance.host
            log.info(s"Instance Registry assigned ElasticSearch instance at $elasticIP")
            Success(instance)
          } recover {case ex =>
            log.warning(s"Failed to read response from Instance Registry, exception: $ex")
            Failure(ex)
          }, Duration.Inf)
        } else if(status == StatusCodes.NotFound) {
          log.warning(s"No matching instance of type 'ElasticSearch' is present at the instance registry.")
          Failure(new RuntimeException(s"Instance Registry did not contain matching instance, server returned $status"))
        } else {
          log.warning(s"Failed to read matching instance from Instance Registry, server returned $status")
          Failure(new RuntimeException(s"Failed to read matching instance from Instance Registry, server returned $status"))
        }
      } recover { case ex =>
        log.warning(s"Failed to request ElasticSearch instance from Instance Registry, exception: $ex ")
        Failure(ex)
      }, Duration.Inf)
    }
  }

  def sendMatchingResult(isElasticSearchReachable : Boolean, configuration: Configuration) : Try[Unit] = {
    if(!configuration.usingInstanceRegistry) {
      Failure(new RuntimeException("Cannot post matching result to Instance Registry, no Instance Registry available."))
    } else {
      if(configuration.elasticsearchInstance.iD.isEmpty) {
        Failure(new RuntimeException("The ElasticSearch instance was not assigned by the Instance Registry, so no matching result will be posted."))
      } else {
        val idToPost = configuration.elasticsearchInstance.iD.getOrElse(-1L)
        val request = HttpRequest(
          method = HttpMethods.POST,
          configuration.instanceRegistryUri + s"/matchingResult?Id=$idToPost&MatchingSuccessful=$isElasticSearchReachable")

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
    }

  }

  def deregister(configuration: Configuration) : Try[Unit] = {
    if(!configuration.usingInstanceRegistry){
      Failure(new RuntimeException("Cannot deregister from Instance Registry, no Instance Registry available."))
    } else {
      val id : Long = configuration.assignedID.getOrElse(-1L)

      val request = HttpRequest(method = HttpMethods.POST, configuration.instanceRegistryUri + s"/deregister?Id=$id")

      Await.result(Http(system).singleRequest(request) map {response =>
        if(response.status == StatusCodes.OK){
          log.info("Successfully deregistered from Instance Registry.")
          Success()
        }
        else {
          val statuscode = response.status
          log.warning(s"Failed to deregister from Instance Registry, server returned $statuscode")
          Failure(new RuntimeException(s"Failed to deregister from Instance Registry, server returned $statuscode"))
        }

      } recover {case ex =>
        log.warning(s"Failed to deregister to Instance Registry, exception: $ex")
        Failure(ex)
      }, Duration.Inf)
    }
  }

  def postInstance(instance : Instance, uri: String) () : Future[HttpResponse] =
    Marshal(instance).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
      Http(system).singleRequest(request)
    }


  private def createInstance(id: Option[Long], controlPort : Int, name : String) : Instance =
    Instance(id, InetAddress.getLocalHost.getHostAddress, controlPort, name, ComponentType.Crawler)
}
