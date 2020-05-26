import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.olownia"
ThisBuild / organizationName := "olownia"

ThisBuild / scalacOptions += "-Wunused"
ThisBuild / scalafixDependencies += organizeImports

lazy val root = (project in file("."))
  .settings(
    name := "BlockchainEtl",
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    libraryDependencies ++= Seq(
      redis,
      fs2,
      catsEffect,
      pureConfig,
      scalaTest % Test
    ) ++ http4s ++ circe ++ doobie ++ logging
  )
