package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._
import de.upb.cs.swt.delphi.crawler.preprocessing.Common._

/**
  * @author Hariharan.
  */
class MavenDownloadActorTest extends TestKit(ActorSystem("DownloadActor"))
                              with ImplicitSender
                              with WordSpecLike
                              with Matchers
                              with BeforeAndAfterAll
{
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The maven download actor" must {
    "create a maven artifact with a jar and pom file" in {
      val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
      val downloadActor = system.actorOf(MavenDownloadActor.props)
      downloadActor ! mavenIdentifier
      val msg = receiveOne(2.seconds)
      assert(msg.isInstanceOf[MavenArtifact])
      val artifact = msg.asInstanceOf[MavenArtifact]
      checkJar(artifact.jarFile.is)
      checkPom(artifact.pomFile.is)
    }
  }
}
