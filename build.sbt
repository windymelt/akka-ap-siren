lazy val ScalaVersion = "2.13.11"
lazy val akkaHttpVersion = "10.5.2"
lazy val akkaVersion = "2.8.3"
lazy val circeVersion = "0.14.5"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.github.windymelt",
      scalaVersion := ScalaVersion
    )
  ),
  name := "akka-ap-siren",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
    "ch.qos.logback" % "logback-classic" % "1.2.11",
    "org.tomitribe" % "tomitribe-http-signatures" % "1.8",
    "com.github.nscala-time" %% "nscala-time" % "2.32.0",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % "test"
  ),
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)
)
