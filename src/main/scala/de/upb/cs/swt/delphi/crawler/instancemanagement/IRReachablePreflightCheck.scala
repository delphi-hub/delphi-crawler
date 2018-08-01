package de.upb.cs.swt.delphi.crawler.instancemanagement

import java.net.InetAddress

import akka.actor.ActorSystem
import scala.util.{Failure, Success}
import de.upb.cs.swt.delphi.crawler.tools.BlockingHttpClient
import de.upb.cs.swt.delphi.crawler.{Configuration, PreflightCheck}
import play.api.libs.json.Json

import scala.util.Try

object IRReachablePreflightCheck extends PreflightCheck{
  override def check(configuration: Configuration)(implicit system: ActorSystem): Try[Configuration] = {
    val fieldList = List(
      ("Type","Crawler"),
      ("IP",InetAddress.getLocalHost().getHostName()),
      ("port",configuration.controlServerPort.toString())
    )
    val json = Json.toJson(fieldList)

    BlockingHttpClient.executePost("/register", json.toString(), response => {
      Success(configuration)
    })(configuration)

    Failure(new IllegalArgumentException(s"Cannot reach instance registry at ${configuration.instanceRegistryUri}"))
  }
}
