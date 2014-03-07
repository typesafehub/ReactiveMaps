name := "reactive-maps"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-contrib" % "2.2.1",
  "com.typesafe.play.extras" %% "play-geojson" % "1.0.0",
  "org.webjars" %% "webjars-play" % "2.2.1",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.8",
  "org.webjars" % "leaflet" % "0.7.2"
)

play.Project.playScalaSettings
