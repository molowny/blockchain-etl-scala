import sbt._

object Dependencies {
  lazy val fs2 = "co.fs2" %% "fs2-core" % "2.2.1"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.3"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  lazy val organizeImports = "com.github.liancheng" %% "organize-imports" % "0.3.0"

  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % "0.12.3")

  lazy val http4s = Seq(
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-blaze-client"
  ).map(_ % "0.21.4")

  lazy val logging = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  )
}
