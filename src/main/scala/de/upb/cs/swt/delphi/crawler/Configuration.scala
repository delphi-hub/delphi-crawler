package de.upb.cs.swt.delphi.crawler

import java.net.URI

import akka.stream.ThrottleMode
import com.sksamuel.elastic4s.ElasticsearchClientUri
import de.upb.cs.swt.delphi.crawler.instancemanagement.InstanceRegistry

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class Configuration {

  lazy val elasticsearchClientUri: ElasticsearchClientUri = ElasticsearchClientUri(InstanceRegistry.retrieveElasticSearchInstance(this) match {
    case Success(elasticIP) => elasticIP
    case Failure(_) => sys.env.getOrElse("DELPHI_ELASTIC_URI","elasticsearch://localhost:9200")
  })

  val mavenRepoBase: URI = new URI("http://repo1.maven.org/maven2/") // TODO: Create a local demo server "http://localhost:8881/maven2/"
  val controlServerPort : Int = 8882
  val throttle : Throttle = Throttle(10, 10 millis, 10, ThrottleMode.shaping)
  val limit : Int = 50

  val instanceName = "MyCrawlerInstance"
  val instanceRegistryUri : String = sys.env.getOrElse("DELPHI_IR_URI", "http://localhost:9500")
  lazy val usingInstanceRegistry = InstanceRegistry.register(this)

  case class Throttle(element : Int, per : FiniteDuration, maxBurst : Int, mode : ThrottleMode)
}

