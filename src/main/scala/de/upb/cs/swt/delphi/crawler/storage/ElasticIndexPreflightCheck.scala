package de.upb.cs.swt.delphi.crawler.storage

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

object ElasticIndexPreflightCheck extends PreflightCheck with ElasticIndexCreator {
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    val client = HttpClient(configuration.elasticsearchClientUri)

    val f = client.execute {
      indexExists("delphi")
    } andThen {
      case _ => client.close()
    }
    val delphiIndexExists: Either[RequestFailure, RequestSuccess[IndexExistsResponse]] = Await.result(f, Duration.Inf)

    delphiIndexExists match {
      case Right(RequestSuccess(404, _, _, _)) => this.createIndex(configuration)
      case Right(_) => // This is fine TODO: Check for index migration
      case Left(RequestFailure(_, _, _, e)) => return Failure(new ElasticException(e))
    }
    println(s"Delphi index exists: $delphiIndexExists")

    Success(configuration)
  }
}
