package de.upb.cs.swt.delphi.crawler.tools

import akka.http.scaladsl.model.StatusCode

class HttpException(code: StatusCode) extends Throwable {

}
