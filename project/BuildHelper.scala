import sbt.Keys._
import sbt._

object BuildHelper {

  def stdSettings(prjName: String) = Seq(
    name := s"$prjName",
    crossScalaVersions := Seq(Scala212, Scala213),
    scalaVersion in ThisBuild := Scala213,
    scalacOptions := CommonOpts ++ extraOptions(scalaVersion.value),
    incOptions ~= (_.withLogRecompileOnMacro(false))
  )

  final private val Scala212 = "2.12.10"
  final private val Scala213 = "2.13.1"

  final private val CommonOpts =
    Seq(
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-Yrangepos",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-Xlint:_,-type-parameter-shadow",
      "-Xsource:2.13",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-unchecked",
      "-deprecation"
    ) ++ customOptions

  final private val Opts213 =
    Seq(
      "-Wunused:imports",
      "-Wvalue-discard",
      "-Wunused:patvars",
      "-Wunused:privates",
      "-Wunused:params",
      "-Wvalue-discard",
      "-Wdead-code"
    )

  final private val OptsTo212 =
    Seq(
      "-Xfuture",
      "-Ypartial-unification",
      "-Ywarn-nullary-override",
      "-Yno-adapted-args",
      "-Ywarn-infer-any",
      "-Ywarn-inaccessible",
      "-Ywarn-nullary-unit",
      "-Ywarn-unused-import"
    )

  private def extraOptions(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        Opts213
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,imports",
          "-Ywarn-unused:imports",
          "-opt:l:inline",
          "-opt-inline-from:<source>"
        ) ++ OptsTo212
      case _ =>
        Seq("-Xexperimental") ++ OptsTo212
    }

  private def propertyFlag(property: String, default: Boolean) =
    sys.props.get(property).map(_.toBoolean).getOrElse(default)

  private def customOptions =
    if (propertyFlag("fatal.warnings", false)) {
      Seq("-Xfatal-warnings")
    } else {
      Nil
    }
}