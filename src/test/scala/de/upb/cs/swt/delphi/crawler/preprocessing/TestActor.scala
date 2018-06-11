package de.upb.cs.swt.delphi.crawler.preprocessing

import java.io.InputStream

import akka.actor.{Actor, ActorLogging, Props}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @author Hariharan.
  */
class TestActor(spec: DispatchActorSpec) extends Actor
  with ActorLogging {
  override def receive: Receive = {
    case j: JarFile => {
      log.info("Received Jar file")
   //   spec.assert(j.isInstanceOf[JarFile])
    }

    case p: PomFile => {
      log.info("Received Pom file")
     // spec.assert(p.isInstanceOf[PomFile])
    }

  }

}

object TestActor {
  def props(spec: DispatchActorSpec) = Props(new TestActor(spec))


}
