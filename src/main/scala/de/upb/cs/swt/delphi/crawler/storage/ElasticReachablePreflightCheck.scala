package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object ElasticReachablePreflightCheck extends PreflightCheck {
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    implicit val ec : ExecutionContext = system.dispatcher
    lazy val client = HttpClient(configuration.elasticsearchClientUri)

    val f = (client.execute {
      nodeInfo()
    } map { i => Success(configuration)
    } recover { case e => Failure(e)
    }).andThen {
      case _ => client.close()
    }

    Await.result(f, Duration.Inf)
  }
}
