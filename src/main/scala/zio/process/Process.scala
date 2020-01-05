package zio.process

import java.io._
import java.lang.{ Process => JProcess }
import java.nio.charset.{ Charset, StandardCharsets }

import zio.blocking._
import zio.stream.{ Stream, StreamChunk, ZSink, ZStream }
import zio.{ RIO, UIO, ZIO }

import scala.collection.mutable

final case class Process(private val process: JProcess) {
  def execute[T](f: JProcess => T): ZIO[Blocking, IOException, T] =
    effectBlocking(f(process)).refineToOrDie[IOException]

  def exitCode: RIO[Blocking, Int] =
    effectBlockingCancelable(process.waitFor())(UIO(process.destroy()))

  def lines: RIO[Blocking, List[String]] = lines(StandardCharsets.UTF_8)

  def lines(charset: Charset): RIO[Blocking, List[String]] =
    effectBlocking { // TODO: effectBlockingCancelable
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream, charset))
      val lines  = new mutable.ArrayDeque[String]

      var line: String = null
      while ({ line = reader.readLine; line != null }) {
        lines.addOne(line)
      }

      lines.toList
    }

  def linesStream: ZStream[Blocking, Throwable, String] =
    stream.chunks
      .aggregate(ZSink.utf8DecodeChunk)
      .aggregate(ZSink.splitLines)
      .mapConcatChunk(identity)

  def stream: StreamChunk[Throwable, Byte] =
    Stream.fromInputStream(process.getInputStream)

  def string: RIO[Blocking, String] = string(StandardCharsets.UTF_8)

  def string(charset: Charset): RIO[Blocking, String] =
    effectBlocking { // TODO: effectBlockingCancelable
      val inputStream = process.getInputStream
      val buffer      = new Array[Byte](4096)
      val result      = new ByteArrayOutputStream
      var length      = 0

      while ({ length = inputStream.read(buffer); length != -1 }) {
        result.write(buffer, 0, length)
      }

      result.toString(charset)
    }.tap(_ => exitCode)
}
