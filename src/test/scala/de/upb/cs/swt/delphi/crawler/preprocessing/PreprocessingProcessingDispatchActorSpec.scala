package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.PreprocessingDispatchActor.{DownloadJar, DownloadPom}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._
import de.upb.cs.swt.delphi.crawler.preprocessing.Common._
/**
  * @author Hariharan.
  */
class PreprocessingProcessingDispatchActorSpec extends TestKit(ActorSystem("DispatchActorSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Dispatch actor" must {
    "download jar file" in {
      val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
      val dispatchActor = system.actorOf(PreprocessingDispatchActor.props(testActor))
      dispatchActor ! DownloadJar(mavenIdentifier)
      val msg=receiveOne(2.seconds)
      assert(msg.isInstanceOf[JarFile])
      val jarFile=msg.asInstanceOf[JarFile]
      checkJar(jarFile.is)
    }
  }
  "A Dispatch actor" must {
    "download pom file" in {
      val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
      val dispatchActor = system.actorOf(PreprocessingDispatchActor.props(testActor))
      dispatchActor ! DownloadPom(mavenIdentifier)
      val msg=receiveOne(2.seconds)
      assert(msg.isInstanceOf[PomFile])
      val pomFile=msg.asInstanceOf[PomFile]
     checkPom(pomFile.is)
    }
  }
}
