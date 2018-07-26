package de.upb.cs.swt.delphi.crawler.processing

import org.opalj.hermes.HermesCore

/**
  * Created by benhermann on 10.05.18.
  */
object Hermes extends HermesCore {

  override def updateProjectData(f: => Unit): Unit = Hermes.synchronized { f }

  override def reportProgress(f: => Double): Unit = Hermes.synchronized { f }
}
