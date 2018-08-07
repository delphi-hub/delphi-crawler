package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, HttpClient}
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceRegistry
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object ElasticReachablePreflightCheck extends PreflightCheck {
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    implicit val ec : ExecutionContext = system.dispatcher
    lazy val client = ElasticClient(configuration.elasticsearchClientUri)

    val f = (client.execute {
      nodeInfo()
    } map { i => {
      if(configuration.usingInstanceRegistry) InstanceRegistry.sendMatchingResult(true, configuration)
      Success(configuration)
    }
    } recover { case e => {
      if(configuration.usingInstanceRegistry) InstanceRegistry.sendMatchingResult(false, configuration)
      Failure(e)
    }
    }).andThen {
      case _ => client.close()
    }

    Await.result(f, Duration.Inf)
  }
}
