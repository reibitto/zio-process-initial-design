name := "zio-process"

version := "0.0.1"

// TODO: Add cross build for other versions + platforms
scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio"          % "1.0.0-RC17",
  "dev.zio" %% "zio-streams"  % "1.0.0-RC17",
  "dev.zio" %% "zio-test"     % "1.0.0-RC17" % Test,
  "dev.zio" %% "zio-test-sbt" % "1.0.0-RC17" % Test
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheckAll")

//parallelExecution := false

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-Yrangepos",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-Xfatal-warnings",
  "-Ywarn-value-discard",
  "-Xlint:infer-any",
  "-Xlint:nullary-unit",
  "-Xlint:nullary-override",
  "-Xlint:inaccessible",
  "-Xlint:missing-interpolator",
  "-Xlint:doc-detached",
  "-Xlint:private-shadow",
  "-Xlint:type-parameter-shadow",
  "-Xlint:delayedinit-select",
  "-Xlint:stars-align",
  "-Xlint:option-implicit",
  "-Xlint:poly-implicit-overload"
)
