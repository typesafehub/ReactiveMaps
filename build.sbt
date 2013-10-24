name := "snapapp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.play.extras" %% "play-geojson" % "1.0.0",
  "com.typesafe.akka" %% "akka-cluster" % "2.2.3",
  // FIXME replace embedded DistributedPubSubMediator "com.typesafe.akka" %% "akka-contrib" % "2.2.3",
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.8",
  "org.webjars" % "leaflet" % "0.6.4",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.3.0"
)

play.Project.playScalaSettings

mappings in Universal ++= (baseDirectory.value / "scripts" * "*" get) map (x => x -> ("scripts/" + x.getName))
