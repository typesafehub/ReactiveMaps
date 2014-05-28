name := "reactive-maps"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.3",
  // FIXME we need this for play 2.3
  //"com.typesafe.play.extras" %% "play-geojson" % "1.0.0",
  "org.webjars" %% "webjars-play" % "2.3.0-RC1-1",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.8",
  "org.webjars" % "leaflet" % "0.7.2"
)

