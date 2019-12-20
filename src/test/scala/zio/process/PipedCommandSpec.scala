package zio.process

import zio.test.Assertion._
import zio.test._

object PipedCommandSpec
    extends DefaultRunnableSpec(
      suite("PipedCommandSpec")(
        testM("support piping") {
          val zio = (Command.of("echo", "2\n1\n3") | Command.of("cat") | Command.of("sort")).lines

          assertM(zio, equalTo(List("1", "2", "3")))
        }
      )
    )
