package zio.process

import zio.blocking.Blocking
import zio.{ IO, ZIO }

case class PipedCommand(commands: Vector[Command]) extends RunnableCommand {
  def run: ZIO[Blocking, Throwable, Process] =
    // TODO: Cleanup and use NonEmptyList of some kind if possible rather than Vector
    commands match {
      case v if v.isEmpty => IO.fail(new Exception("No processes to run"))
      case Vector(head)   => head.run
      case Vector(head, tail @ _*) =>
        for {
          stream <- tail.init.foldLeft(head.stream) {
                     case (s, command) =>
                       s.flatMap { input =>
                         command.stdin(StreamingInput(input)).stream
                       }
                   }
          result <- tail.last.stdin(StreamingInput(stream)).run
        } yield result
    }

  def pipe(into: Command): PipedCommand =
    copy(commands :+ into)

  def |(into: Command): PipedCommand =
    pipe(into)
}

object PipedCommand {
  def of(command: String*): PipedCommand =
    PipedCommand(Vector(Command.of(command: _*)))
}
