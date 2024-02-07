ThisBuild / scalaVersion := "2.13.12"

ThisBuild / version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """gwi-test""",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
    )
  )

libraryDependencies ++= Seq(
  "com.opencsv" % "opencsv" % "5.7.1",
  "com.typesafe.play" %% "play-json" % "2.9.4"
)

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.9.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.10"

libraryDependencies += "com.typesafe.play" %% "play-ahc-ws" % "2.8.18"  // Adjust the version accordingly

libraryDependencies += "com.typesafe.play" %% "play" % "2.8.12"


