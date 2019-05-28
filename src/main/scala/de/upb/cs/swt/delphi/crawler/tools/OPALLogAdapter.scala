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
