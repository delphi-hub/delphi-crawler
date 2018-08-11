package de.upb.cs.swt.delphi.crawler

import akka.actor.ActorSystem

import scala.util.Try

trait PreflightCheck {
  def check(configuration: Configuration)(implicit system: ActorSystem) : Try[Configuration]
}
