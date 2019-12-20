package zio.process

import java.io.File

sealed trait ProcessOutput

case class FileRedirect(file: File)       extends ProcessOutput
case class FileAppendRedirect(file: File) extends ProcessOutput
case object InheritOutput                 extends ProcessOutput
case object PipeOutput                    extends ProcessOutput
