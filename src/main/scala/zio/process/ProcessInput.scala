package zio.process

import java.io.ByteArrayInputStream
import java.nio.charset.{ Charset, StandardCharsets }

import zio.Chunk
import zio.stream.{ Stream, StreamChunk }

trait ProcessInput {
  def source: StreamChunk[Throwable, Byte]
}

case class StreamingInput(stream: StreamChunk[Throwable, Byte]) extends ProcessInput {
  def source: StreamChunk[Throwable, Byte] = stream
}

case class StringInput(text: String, charset: Charset = StandardCharsets.UTF_8) extends ProcessInput {
  def source: StreamChunk[Throwable, Byte] = StreamChunk.fromChunks(Chunk.fromArray(text.getBytes(charset)))
}

case class ByteArrayInput(bytes: Array[Byte]) extends ProcessInput {
  def source: StreamChunk[Throwable, Byte] = Stream.fromInputStream(new ByteArrayInputStream(bytes))
}

case object InheritInput extends ProcessInput {
  def source: StreamChunk[Throwable, Byte] = StreamChunk.empty
}
