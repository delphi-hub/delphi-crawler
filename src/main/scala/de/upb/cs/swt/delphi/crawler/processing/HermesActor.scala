package de.upb.cs.swt.delphi.crawler.processing

import java.net.URL
import java.util.jar.JarInputStream

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenArtifact
import de.upb.cs.swt.delphi.crawler.tools.ClassStreamReader
import org.opalj.hermes.{Feature, FeatureQuery}

class HermesActor(elasticActor : ActorRef) extends Actor with ActorLogging {


  def transformToFeatures(results: Iterator[(FeatureQuery, TraversableOnce[Feature[URL]])]) = {
    results.flatMap{ case(query, features: TraversableOnce[Feature[URL]]) => features.map { case feature => feature.id -> feature.count}} toMap
  }

  def receive = {
    case m : MavenArtifact => {
      log.info(s"Starting analysis for $m")
      val project = new ClassStreamReader{}.createProject(m.identifier.toJarLocation.toURL,
                                            new JarInputStream(m.jarFile.is))

      val results = new HermesAnalyzer(project).analyzeProject()

      val featureMap = transformToFeatures(results)

      val hermesResult = HermesResults(m.identifier, featureMap)

      log.info(s"Feature map for ${m.identifier.toUniqueString} is ${featureMap.size}.")
      elasticActor ! hermesResult
    }

  }
}

object HermesActor {
  type HermesStatistics = scala.collection.Map[String, Double]
  def props(elasticActor : ActorRef) = Props(new HermesActor(elasticActor))

}

case class HermesResults(identifier: MavenIdentifier, featureMap: Map[String, Int])