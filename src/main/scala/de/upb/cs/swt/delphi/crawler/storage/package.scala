package de.upb.cs.swt.delphi.crawler

import com.sksamuel.elastic4s.IndexAndType

package object storage {
  val delphi = "delphi"
  val project = "project"
  val delphiProjectType: IndexAndType = IndexAndType(delphi,project)
}
