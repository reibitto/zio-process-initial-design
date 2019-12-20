package zio.process

import java.io.{ File, IOException }
import java.nio.charset.StandardCharsets

import zio.stream.ZSink
import zio.test.Assertion._
import zio.test._
import zio.duration._
import zio.test.environment.TestClock

// TODO: Add aspects for different OSes? scala.util.Properties.isWin, etc. Also try to make this as OS agnostic as possible in the first place
object CommandSpec
    extends DefaultRunnableSpec(
      suite("CommandSpec")(
        testM("convert stdout to string") {
          val zio = Command.of("echo", "-n", "test").string

          assertM(zio, equalTo("test"))
        },
        testM("convert stdout to list of lines") {
          val zio = Command.of("echo", "-n", "1\n2\n3").lines

          assertM(zio, equalTo(List("1", "2", "3")))
        },
        testM("work with stream directly") {
          val zio = for {
            stream <- Command.of("echo", "-n", "1\n2\n3").stream
            lines <- stream.chunks
                      .aggregate(ZSink.utf8DecodeChunk)
                      .aggregate(ZSink.splitLines)
                      .mapConcatChunk(identity)
                      .runCollect
          } yield lines

          assertM(zio, equalTo(List("1", "2", "3")))
        },
        testM("fail trying to run a command that doesn't exit") {
          val zio = Command.of("some-invalid-command", "test").string

          assertM(zio.run, fails(isSubtype[IOException](anything)))
        },
        testM("pass environment variables") {
          val zio = Command.of("bash", "-c", "echo -n \"var = $VAR\"").env(Map("VAR" -> "value")).string

          assertM(zio, equalTo("var = value"))
        },
        testM("accept streaming stdin") {
          val zio = for {
            stream <- Command.of("echo", "-n", "a", "b", "c").stream
            result <- Command.of("cat").stdin(StreamingInput(stream)).string
          } yield result

          assertM(zio, equalTo("a b c"))
        },
        testM("accept string stdin") {
          val zio = Command.of("cat").stdin(StringInput("piped in")).string

          assertM(zio, equalTo("piped in"))
        },
        testM("support different encodings") {
          val zio =
            Command.of("cat").stdin(StringInput("piped in", StandardCharsets.UTF_16)).string(StandardCharsets.UTF_16)

          assertM(zio, equalTo("piped in"))
        },
        testM("set workingDirectory") {
          val zio = Command.of("ls").workingDirectory(new File("src/main/scala/zio/process")).lines

          assertM(zio, contains("Command.scala"))
        },
        testM("interrupt a process manually") {
          val zio = for {
            fiber  <- Command.of("sleep", "20").exitCode.fork
            _      <- fiber.interrupt.fork
            result <- fiber.join
          } yield result

          assertM(zio.run, isInterrupted)
        },
        testM("interrupt a process due to timeout") {
          val zio = for {
            fiber  <- Command.of("sleep", "20").exitCode.timeout(5.seconds).fork
            _      <- TestClock.adjust(5.seconds)
            result <- fiber.join
          } yield result

          assertM(zio, isNone)
        }
      )
    )
