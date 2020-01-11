import BuildHelper._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(Developer("jdegoes", "John De Goes", "john@degoes.net", url("http://degoes.net"))),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/zio-process/"), "scm:git:git@github.com:zio/zio-process.git")
    )
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val `zio-process` = project
  .in(file("."))
  .settings(stdSettings("zio-process"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"                %% "zio"                     % "1.0.0-RC17",
      "dev.zio"                %% "zio-streams"             % "1.0.0-RC17",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
      "dev.zio"                %% "zio-test"                % "1.0.0-RC17" % Test,
      "dev.zio"                %% "zio-test-sbt"            % "1.0.0-RC17" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
