package zio.process

import java.io.ByteArrayInputStream
import java.nio.charset.{ Charset, StandardCharsets }

import zio.Chunk
import zio.stream.{ Stream, StreamChunk }

final case class ProcessInput(source: Option[StreamChunk[Throwable, Byte]])

object ProcessInput {
  val inherit: ProcessInput = ProcessInput(None)

  def fromByteArray(bytes: Array[Byte]): ProcessInput =
    ProcessInput(Some(Stream.fromInputStream(new ByteArrayInputStream(bytes))))

  def fromStreamChunk(stream: StreamChunk[Throwable, Byte]): ProcessInput =
    ProcessInput(Some(stream))

  def fromString(text: String, charset: Charset): ProcessInput =
    ProcessInput(Some(StreamChunk.fromChunks(Chunk.fromArray(text.getBytes(charset)))))

  def fromUTF8String(text: String): ProcessInput =
    ProcessInput(Some(StreamChunk.fromChunks(Chunk.fromArray(text.getBytes(StandardCharsets.UTF_8)))))
}
