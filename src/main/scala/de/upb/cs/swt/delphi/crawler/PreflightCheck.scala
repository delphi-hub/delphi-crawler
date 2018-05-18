package de.upb.cs.swt.delphi.crawler

import scala.concurrent.ExecutionContext
import scala.util.Try

trait PreflightCheck {
  def check(configuration: Configuration)(implicit context : ExecutionContext) : Try[Configuration]
}
