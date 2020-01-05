package zio.process

import zio.test.Assertion._
import zio.test._

object PipedCommandSpec
    extends DefaultRunnableSpec(
      suite("PipedCommandSpec")(
        testM("support piping") {
          val zio = (Command("echo", "2\n1\n3") | Command("cat") | Command("sort")).lines

          assertM(zio, equalTo(List("1", "2", "3")))
        },
        testM("piping is associative") {
          for {
            lines1 <- (Command("echo", "2\n1\n3") | (Command("cat") | (Command("sort") | Command("head", "-2")))).lines
            lines2 <- (Command("echo", "2\n1\n3") | Command("cat") | (Command("sort") | Command("head", "-2"))).lines
          } yield assert(lines1, equalTo(lines2))
        },
        testM("stdin on piped command") {
          val zio = (Command("cat") | Command("sort") | (Command("head", "-2")))
            .stdin(ProcessInput.fromUTF8String("2\n1\n3"))
            .lines

          assertM(zio, equalTo(List("1", "2")))
        }
      )
    )
