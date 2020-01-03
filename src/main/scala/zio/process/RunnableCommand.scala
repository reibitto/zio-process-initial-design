package zio.process

import java.nio.charset.Charset

import zio.RIO
import zio.blocking.Blocking
import zio.stream.{ StreamChunk, ZStream }

trait RunnableCommand {
  def run: RIO[Blocking, Process]

  def exitCode: RIO[Blocking, Int] =
    run.flatMap(_.exitCode)

  def string: RIO[Blocking, String] =
    run.flatMap(_.string)

  def string(charset: Charset): RIO[Blocking, String] =
    run.flatMap(_.string(charset))

  def lines: RIO[Blocking, List[String]] =
    run.flatMap(_.lines)

  def lines(charset: Charset): RIO[Blocking, List[String]] =
    run.flatMap(_.lines(charset))

  def linesStream: ZStream[Blocking, Throwable, String] =
    ZStream.fromEffect(run).flatMap(_.linesStream)

  def stream: RIO[Blocking, StreamChunk[Throwable, Byte]] =
    run.map(_.stream)
}
