package de.upb.cs.swt.delphi.crawler.tools

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import de.upb.cs.swt.delphi.crawler.Configuration

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/***
  * A blocking http client implemented using Akka HTTP
  */
object BlockingHttpClient {

  def doRequest(uri : Uri, method : HttpMethod, jsonData : String) : Try[String] = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    val httpActor = Http(system)

    try {
      println("Creating request...")
      val request = HttpRequest(method = method, uri = uri, entity = ByteString(jsonData))
      println("Executing request...")
      val req: Future[HttpResponse] = httpActor.singleRequest(request)
      println("Awatiting response...")
      Await.result(req, Duration.Inf)

      println("Parsing response...")

      val f = req.value.get.get.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
      Await.result(f, Duration.Inf)

      Success(f.value.get.get.utf8String)
    } catch  {
      case e : Exception => {
        httpActor.shutdownAllConnectionPools()
        system.terminate()
        Failure(e)
      }
    }

  }

  def executeRequest(target: String, method : HttpMethod, jsonData: String, onSuccess: String => Unit)(config: Configuration) : Unit = {

    val uri = Uri(config.instanceRegistryUri)
    println(s"Contacting server ${config.instanceRegistryUri}...")
    val resp = BlockingHttpClient.doRequest(uri.withPath(uri.path + target), method, jsonData)

    resp match {
      case Success(res) => onSuccess(res)
      case Failure(_) => println(s"Could not reach server ${config.instanceRegistryUri}.")
    }
  }

  def executeGet(target: String, jsonData: String, onSuccess: String => Unit)(config: Configuration): Unit = executeRequest(target, HttpMethods.GET, jsonData, onSuccess)(config)
  def executePost(target: String, jsonData: String,  onSuccess: String => Unit)(config: Configuration): Unit = executeRequest(target, HttpMethods.POST, jsonData, onSuccess)(config)


}
