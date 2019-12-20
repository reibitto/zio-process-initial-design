package zio.process

import java.io.File
import java.lang.ProcessBuilder.Redirect

import zio.blocking.Blocking
import zio.stream.ZSink
import zio.{ RIO, Task, UIO, ZIO }

import scala.jdk.CollectionConverters._

case class Command(
  command: List[String],
  env: Map[String, String],
  workingDirectory: Option[File],
  stdin: Option[ProcessInput],
  stdout: Option[ProcessOutput],
  stderr: Option[ProcessOutput],
  redirectErrorStream: Boolean
) extends RunnableCommand {
  def env(env: Map[String, String]): Command = copy(env = env)

  def workingDirectory(directory: File): Command = copy(workingDirectory = Some(directory))

  def stdin(stdin: ProcessInput): Command = copy(stdin = Some(stdin))

  def stdout(stdout: ProcessOutput): Command = copy(stdout = Some(stdout))

  def stderr(stderr: ProcessOutput): Command = copy(stderr = Some(stderr))

  def redirectErrorStream(redirectErrorStream: Boolean): Command = copy(redirectErrorStream = redirectErrorStream)

  def run: RIO[Blocking, Process] =
    for {
      process <- Task {
                  val builder = new ProcessBuilder(command: _*)
                  builder.redirectErrorStream(redirectErrorStream)
                  workingDirectory.foreach(builder.directory)

                  if (env.nonEmpty) {
                    builder.environment().putAll(env.asJava)
                  }

                  stdin.foreach {
                    case InheritInput => builder.redirectInput(Redirect.INHERIT)
                    case _            => ()
                  }

                  stdout.foreach {
                    case FileRedirect(file)       => builder.redirectOutput(Redirect.to(file))
                    case FileAppendRedirect(file) => builder.redirectOutput(Redirect.appendTo(file))
                    case InheritOutput            => builder.redirectOutput(Redirect.INHERIT)
                    case PipeOutput               => builder.redirectOutput(Redirect.PIPE)
                  }

                  stderr.foreach {
                    case FileRedirect(file)       => builder.redirectError(Redirect.to(file))
                    case FileAppendRedirect(file) => builder.redirectError(Redirect.appendTo(file))
                    case InheritOutput            => builder.redirectError(Redirect.INHERIT)
                    case PipeOutput               => builder.redirectError(Redirect.PIPE)
                  }

                  Process(builder.start())
                }
      _ <- stdin match {
            case None | Some(InheritInput) => ZIO.unit
            case Some(input) =>
              input.source.chunks
                .run(ZSink.fromOutputStream(process.stdout))
                .ensuring(UIO(process.stdout.close()))
                .fork
                .daemon
          }
    } yield process

  def pipe(into: Command): PipedCommand =
    PipedCommand(Vector(this, into))

  def |(into: Command): PipedCommand =
    pipe(into)

  def inheritIO: Command =
    copy(
      stdin = Some(InheritInput),
      stdout = Some(InheritOutput),
      stderr = Some(InheritOutput)
    )
}

object Command {
  def of(command: String*): Command =
    Command(
      command.toList,
      Map.empty,
      Option.empty[File],
      Option.empty[ProcessInput],
      Option.empty[ProcessOutput],
      Option.empty[ProcessOutput], // TODO: Should we inherit stderr by default?
      redirectErrorStream = false
    )
}
