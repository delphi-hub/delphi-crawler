package de.upb.cs.swt.delphi.crawler.discovery.maven

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}

import scala.concurrent.duration._

object Playground extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val ip = new IndexProcessing {}
  ip.createSource()
    .throttle(1, 1.second, 1, ThrottleMode.shaping)
    .runForeach(println)
}
