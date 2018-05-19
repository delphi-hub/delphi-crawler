package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object ElasticReachablePreflightCheck extends PreflightCheck {
  override def check(configuration: Configuration)(implicit context: ExecutionContext): Try[Configuration] = {
    val client = HttpClient(configuration.elasticsearchClientUri)

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
