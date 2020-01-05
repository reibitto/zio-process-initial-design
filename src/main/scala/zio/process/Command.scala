package zio.process

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.charset.Charset

import zio.blocking.Blocking
import zio.process.Command.Piped
import zio.stream.{ StreamChunk, ZSink, ZStream }
import zio.{ RIO, Task, UIO, ZIO }

import scala.jdk.CollectionConverters._

sealed trait Command {

  def env(env: Map[String, String]): Command =
    transform {
      case c: Command.Standard => c.copy(env = env)
      case c: Command.Piped    => c
    }

  def exitCode: RIO[Blocking, Int] =
    run.flatMap(_.exitCode)

  def inheritIO: Command =
    transform {
      case c: Command.Standard =>
        c.copy(
          stdin = ProcessInput.inherit,
          stdout = ProcessOutput.Inherit,
          stderr = ProcessOutput.Inherit
        )
      case c: Command.Piped => c
    }

  def lines: RIO[Blocking, List[String]] =
    run.flatMap(_.lines)

  def lines(charset: Charset): RIO[Blocking, List[String]] =
    run.flatMap(_.lines(charset))

  def linesStream: ZStream[Blocking, Throwable, String] =
    ZStream.fromEffect(run).flatMap(_.linesStream)

  def pipe(into: Command): Piped =
    Piped(this, into)

  def |(into: Command): Piped =
    pipe(into)

  def redirectErrorStream(redirectErrorStream: Boolean): Command =
    transform {
      case c: Command.Standard => c.copy(redirectErrorStream = redirectErrorStream)
      case c: Command.Piped    => c
    }

  def run: RIO[Blocking, Process] =
    this match {
      case c: Command.Standard =>
        for {
          process <- Task {
                      val builder = new ProcessBuilder(c.command: _*)
                      builder.redirectErrorStream(c.redirectErrorStream)
                      c.workingDirectory.foreach(builder.directory)

                      if (c.env.nonEmpty) {
                        builder.environment().putAll(c.env.asJava)
                      }

                      c.stdin match {
                        case ProcessInput(None)    => builder.redirectInput(Redirect.INHERIT)
                        case ProcessInput(Some(_)) => ()
                      }

                      c.stdout match {
                        case ProcessOutput.FileRedirect(file)       => builder.redirectOutput(Redirect.to(file))
                        case ProcessOutput.FileAppendRedirect(file) => builder.redirectOutput(Redirect.appendTo(file))
                        case ProcessOutput.Inherit                  => builder.redirectOutput(Redirect.INHERIT)
                        case ProcessOutput.Pipe                     => builder.redirectOutput(Redirect.PIPE)
                      }

                      c.stderr match {
                        case ProcessOutput.FileRedirect(file)       => builder.redirectError(Redirect.to(file))
                        case ProcessOutput.FileAppendRedirect(file) => builder.redirectError(Redirect.appendTo(file))
                        case ProcessOutput.Inherit                  => builder.redirectError(Redirect.INHERIT)
                        case ProcessOutput.Pipe                     => builder.redirectError(Redirect.PIPE)
                      }

                      Process(builder.start())
                    }
          _ <- c.stdin match {
                case ProcessInput(None) => ZIO.unit
                case ProcessInput(Some(input)) =>
                  for {
                    outputStream <- process.execute(_.getOutputStream)
                    _ <- input.chunks
                          .run(ZSink.fromOutputStream(outputStream))
                          .ensuring(UIO(outputStream.close()))
                          .fork
                          .daemon
                  } yield ()
              }
        } yield process

      case Command.Piped(from: Command, to: Command.Standard) =>
        for {
          s      <- from.stream
          result <- to.stdin(ProcessInput.fromStreamChunk(s)).run
        } yield result

      case Command.Piped(from: Command.Standard, to: Command.Piped) =>
        for {
          s1     <- from.stream
          s2     <- to.left.stdin(ProcessInput.fromStreamChunk(s1)).stream
          result <- to.right.stdin(ProcessInput.fromStreamChunk(s2)).run
        } yield result

      case Command.Piped(from: Command.Piped, to: Command.Piped) =>
        for {
          s1     <- from.left.stream
          s2     <- from.right.stdin(ProcessInput.fromStreamChunk(s1)).stream
          s3     <- to.left.stdin(ProcessInput.fromStreamChunk(s2)).stream
          result <- to.right.stdin(ProcessInput.fromStreamChunk(s3)).run
        } yield result
    }

  def stdin(stdin: ProcessInput): Command = this match {
    case c: Command.Standard =>
      c.copy(stdin = stdin)

    // For piped commands it only makes sense to provide `stdin` for the leftmost command as the rest will be piped in.
    case c @ Command.Piped(left: Command.Standard, _) =>
      c.copy(left = left.copy(stdin = stdin))

    case Command.Piped(left: Command.Piped, right) =>
      Command.Piped(Command.Piped(left.left.stdin(stdin), left.right), right)
  }

  def stderr(stderr: ProcessOutput): Command =
    transform {
      case c: Command.Standard => c.copy(stderr = stderr)
      case c: Command.Piped    => c
    }

  def stdout(stdout: ProcessOutput): Command = transform {
    case c: Command.Standard => c.copy(stdout = stdout)
    case c: Command.Piped    => c
  }

  def string: RIO[Blocking, String] =
    run.flatMap(_.string)

  def string(charset: Charset): RIO[Blocking, String] =
    run.flatMap(_.string(charset))

  def stream: RIO[Blocking, StreamChunk[Throwable, Byte]] =
    run.map(_.stream)

  def transform(f: Command => Command): Command = this match {
    case c: Command.Standard =>
      f(c)

    case Command.Piped(left, right) =>
      Command.Piped(left.transform(f), right.transform(f))
  }

  def workingDirectory(directory: File): Command = transform {
    case c: Command.Standard => c.copy(workingDirectory = Some(directory))
    case c: Command.Piped    => c
  }

  // TODO: Add other common bash operators that might make sense. >, >>, &&, etc

}

object Command {

  final case class Standard(
    command: ::[String],
    env: Map[String, String],
    workingDirectory: Option[File],
    stdin: ProcessInput,
    stdout: ProcessOutput,
    stderr: ProcessOutput,
    redirectErrorStream: Boolean
  ) extends Command

  final case class Piped(left: Command, right: Command) extends Command

  def apply(processName: String, args: String*): Command.Standard =
    Command.Standard(
      ::(processName, args.toList),
      Map.empty,
      Option.empty[File],
      ProcessInput.inherit,
      ProcessOutput.Pipe,
      ProcessOutput.Inherit,
      redirectErrorStream = false
    )
}
