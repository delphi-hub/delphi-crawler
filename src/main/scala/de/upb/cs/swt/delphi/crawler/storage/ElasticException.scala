package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.http.ElasticError

class ElasticException(val error : ElasticError) extends Throwable {
  override def getMessage: String = error.reason
}
