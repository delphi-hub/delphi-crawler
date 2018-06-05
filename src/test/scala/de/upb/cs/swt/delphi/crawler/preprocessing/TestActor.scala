package de.upb.cs.swt.delphi.crawler.preprocessing

import java.io.InputStream

import akka.actor.{Actor, ActorLogging, Props}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @author Hariharan.
  */
class TestActor extends Actor
  with ActorLogging {
  override def receive: Receive = {
    case j: JarFile => {
      log.info("Received Jar file")

    }

    case p: PomFile => {
      log.info("Received Pom file")

    }

  }

}

object TestActor {
  def props = Props(new TestActor())


}
