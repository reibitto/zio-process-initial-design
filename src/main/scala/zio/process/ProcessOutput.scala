package zio.process

import java.io.File

sealed trait ProcessOutput

object ProcessOutput {
  final case class FileRedirect(file: File)       extends ProcessOutput
  final case class FileAppendRedirect(file: File) extends ProcessOutput
  case object Inherit                             extends ProcessOutput
  case object Pipe                                extends ProcessOutput
}
