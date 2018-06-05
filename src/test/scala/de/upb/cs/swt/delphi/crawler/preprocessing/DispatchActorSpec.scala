package de.upb.cs.swt.delphi.crawler.preprocessing

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.DispatchActor.{DownloadJar, DownloadPom}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @author Hariharan.
  */
class DispatchActorSpec extends TestKit(ActorSystem("DispatchActorSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Dispatch actor" must {
    "download jar file" in {
      val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
      val testActorRef=system.actorOf(TestActor.props)
      val dispatchActor = system.actorOf(DispatchActor.props(testActorRef))
      dispatchActor ! DownloadJar(mavenIdentifier)

    }
  }
  "A Dispatch actor" must {
    "download pom file" in {
      val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
      val testActorRef=system.actorOf(TestActor.props)
      val dispatchActor = system.actorOf(DispatchActor.props(testActorRef))
      dispatchActor ! DownloadPom(mavenIdentifier)
    }
  }
}
