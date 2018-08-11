package de.upb.cs.swt.delphi.crawler

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.event.{BusLogging, LoggingAdapter}

trait AppLogging {
  def log(implicit system: ActorSystem): LoggingAdapter = new BusLogging(system.eventStream, this.getClass.getName, this.getClass, system.asInstanceOf[ExtendedActorSystem].logFilter)
}
