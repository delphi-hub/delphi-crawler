// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.tools

import org.opalj.log.{DevNullLogger, Fatal, GlobalLogContext, Info, LogContext, LogMessage, OPALLogger, StandardLogContext, Warn}
import org.slf4j.LoggerFactory

/**
  * Custom OPALLogger implementation that forwards messages to the internal logger
  *
  * @author Johannes Düsing
  */
object OPALLogAdapter extends OPALLogger {

  private val internalLogger = LoggerFactory.getLogger("opal-logger")
  private var opalLoggingEnabled = false

  final val emptyLogger = DevNullLogger
  final val consoleLogger = OPALLogAdapter

  final val analysisLogContext = new StandardLogContext()

  OPALLogger.register(analysisLogContext, emptyLogger)
  OPALLogger.updateLogger(GlobalLogContext, emptyLogger)


  override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
    message.level match {
      case Info =>
        internalLogger.info(message.message)
      case Warn =>
        internalLogger.warn(message.message)
      case org.opalj.log.Error =>
        internalLogger.error(message.message)
      case Fatal =>
        internalLogger.error(message.message)
    }
  }

  /**
    * Logger currently used by the analysis framework
    */
  def analysisLogger: OPALLogger = if(opalLoggingEnabled) consoleLogger else emptyLogger

  /**
    * Method that enables or disables OPAL logging entirely. If enabled, all log levels of OPAL will
    * be forwarded to the internal analysis logger. If disabled, all logging output of OPAL will be
    * suppressed.
    *
    * @param enabled Parameter indicating whether to enable OPAL logging
    */
  def setOpalLoggingEnabled(enabled: Boolean): Unit = {
    opalLoggingEnabled = enabled

    OPALLogger.updateLogger(GlobalLogContext, analysisLogger)

    if(OPALLogger.isUnregistered(analysisLogContext)) {
      OPALLogger.register(analysisLogContext, analysisLogger)
    } else {
      OPALLogger.updateLogger(analysisLogContext, analysisLogger)
    }
  }

}