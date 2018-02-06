package de.upb.cs.swt.delphi.crawler.hermes

import java.io.File
import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Identifier
import de.upb.cs.swt.delphi.crawler.hermes.HermesActor.{Analyze, HermesStatistics, ProcessResults, ProcessStatistics}
import org.opalj.hermes.{HermesCore, ProjectFeatures}


/**
  * Created by benhermann on 06.02.18.
  */
class HermesActor extends Actor with ActorLogging {


  def receive = {
    case Analyze(i : Identifier, b : BinaryPackage) => {
      log.info("Starting Hermes analysis for {}", i)

      Hermes.analysesFinished onChange { (_, _, isFinished) ⇒
        if (isFinished) {
          self ! ProcessStatistics(i, Hermes.featureMatrix.head.projectConfiguration.statistics)
          self ! ProcessResults(i, Hermes.featureMatrix.head)
        }
      }
      // Fake some kind of config file here.
      Hermes.initialize(new File(""))
      Hermes.analyzeCorpus(runAsDaemons = false)
    }
    case ProcessStatistics(i : Identifier, results : HermesStatistics) => {
      log.info("Processing Hermes statistics data for {}", i)
      // convert
    }
    case ProcessResults(i : Identifier, results : ProjectFeatures[URL]) => {
      log.info("Processing Hermes results for {}", i)
      // convert results and push to elastic
    }

  }
}

object HermesActor {
  type HermesStatistics = scala.collection.Map[String, Double]
  def props = Props(new HermesActor)

  case class Analyze(i : Identifier, binaryPackage : BinaryPackage)
  case class ProcessResults(i : Identifier, results : ProjectFeatures[URL])
  case class ProcessStatistics(i : Identifier, stats : HermesStatistics)
}



class BinaryPackage

object Hermes extends HermesCore {

  override def updateProjectData(f: ⇒ Unit): Unit = Hermes.synchronized { f }

  override def reportProgress(f: ⇒ Double): Unit = Hermes.synchronized { f }
}