import sbt._
import ByteConversions._

name := "reactive-maps"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, PlayScala, SbtTypesafeConductR)

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-contrib" % "2.3.7",
  "com.typesafe.play.extras" %% "play-geojson" % "1.1.0",

  "com.typesafe.conductr"    %% "akka-conductr-bundle-lib" % "0.6.1",
  "com.typesafe.conductr"    %% "play-conductr-bundle-lib" % "0.6.1",

  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout"  % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "leaflet"   % "0.7.2",
  "org.webjars" % "rjs"       % "2.1.11-1-trireme" % "test",
  "org.webjars" % "squirejs"  % "0.1.0" % "test"
)

resolvers += "typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases" // for conductr-bundle-lib

scalacOptions += "-feature"

MochaKeys.requires += "SetupMocha.js"

pipelineStages := Seq(rjs, digest, gzip)

// bundle settings
BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 64.MiB
BundleKeys.diskSpace := 5.MB
BundleKeys.endpoints := Map("web" -> Endpoint("http", 0, Set(URI("http://:9000"))))
BundleKeys.startCommand += "-Dhttp.address=$WEB_BIND_IP -Dhttp.port=$WEB_BIND_PORT"
