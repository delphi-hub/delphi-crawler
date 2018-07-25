package de.upb.cs.swt.delphi.crawler.tools

import org.opalj.log.{LogContext, LogMessage, OPALLogger}
import org.slf4j.LoggerFactory

object OPALLogAdapter extends OPALLogger  {

  val l = LoggerFactory.getLogger(this.getClass)

  override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
    message.level match {
      case org.opalj.log.Info => l.info(message.message)
      case org.opalj.log.Warn => l.warn(message.message)
      case org.opalj.log.Error => l.error(message.message)

    }
  }
}
