package de.upb.cs.swt.delphi.crawler.control

import java.io.{BufferedReader, PrintStream}

object CLI extends CLI {

}

class CLI extends REPL(Console.in, Console.out) {

}

abstract class REPL(in : BufferedReader, out : PrintStream) {
  val prompt = "> "

  private def read() : Command = {
    out.println(prompt)
    val commandString = in.readLine()
    interpretCommand(commandString)
  }

  protected def interpretCommand(commandString: String): Command = {
    commandString match {
      case x if x.isEmpty => EmptyCommand()
      case "quit" => QuitCommand()
      case _ => UnrecognizedCommand(commandString)
    }
  }

  private def runCommand(command : Command): Unit = {
    command.run(out)
  }

  def start() = {
    while(true) {
      val currentCommand = read()
      runCommand(currentCommand)
    }
  }
}

trait Command {
  def run(out : PrintStream)
}

case class EmptyCommand() extends Command {
  override def run(out: PrintStream): Unit = {}
}

case class QuitCommand() extends Command {
  override def run(out: PrintStream): Unit = {
    out.println("Shutting down... Good bye!")
    sys.exit(0)
  }
}

case class UnrecognizedCommand(commandString: String) extends Command {
  override def run(out: PrintStream): Unit = {
    out.println(s"Unrecognized command: $commandString")
  }
}