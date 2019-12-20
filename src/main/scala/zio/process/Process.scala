package zio.process

import java.io._
import java.lang.{ Process => JProcess }
import java.nio.charset.{ Charset, StandardCharsets }

import zio.blocking._
import zio.stream.{ Stream, StreamChunk }
import zio.{ RIO, UIO }

import scala.collection.mutable

case class Process(process: JProcess) {
  def stdin: InputStream = process.getInputStream

  def stdout: OutputStream = process.getOutputStream

  def stderr: InputStream = process.getErrorStream

  def exitCode: RIO[Blocking, Int] =
    effectBlockingCancelable(process.waitFor())(UIO(process.destroy()))

  def string: RIO[Blocking, String] = string(StandardCharsets.UTF_8)

  def string(charset: Charset): RIO[Blocking, String] =
    effectBlocking {
      val inputStream = process.getInputStream
      val buffer      = new Array[Byte](4096)
      val result      = new ByteArrayOutputStream
      var length      = 0

      while ({ length = inputStream.read(buffer); length != -1 }) {
        result.write(buffer, 0, length)
      }

      result.toString(charset)
    }.tap(_ => exitCode)

  def lines: RIO[Blocking, List[String]] = lines(StandardCharsets.UTF_8)

  def lines(charset: Charset): RIO[Blocking, List[String]] =
    effectBlocking {
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream, charset))
      val lines  = new mutable.ArrayDeque[String]

      var line: String = null
      while ({ line = reader.readLine; line != null }) {
        lines.addOne(line)
      }

      lines.toList
    }

  def stream: StreamChunk[Throwable, Byte] =
    Stream.fromInputStream(process.getInputStream)
}
