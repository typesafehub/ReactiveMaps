import ByteConversions._

name := "reactive-maps"
organization in ThisBuild := "com.typesafe"
version := "1.0-SNAPSHOT"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  TypesafeLibrary.akkaOrganization %% "akka-contrib" % "2.3.11",
  "com.typesafe.play.extras" %% "play-geojson" % "1.3.0",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "leaflet" % "0.7.2",
  "org.webjars" % "rjs" % "2.1.11-1-trireme" % "test",
  "org.webjars" % "squirejs" % "0.1.0" % "test"
)

routesGenerator := InjectedRoutesGenerator

scalacOptions += "-feature"

MochaKeys.requires += "SetupMocha.js"

pipelineStages := Seq(rjs, digest, gzip)


// Main bundle configuration

normalizedName in Bundle := "reactive-maps-frontend"

BundleKeys.system := "reactive-maps"

BundleKeys.endpoints := Map(
  "web" -> Endpoint("http", 0, serviceName = "reactive-maps-frontend", acls =
    RequestAcl(
      Http(
        "^/".r
      )
    )
  ),
  "akka-remote" -> Endpoint("tcp", 0)
)

BundleKeys.roles := Set("dmz")
BundleKeys.startCommand += "-Dakka.cluster.roles.1=frontend"

// Bundles that override the main one

lazy val BackendRegion = config("backend-region").extend(Bundle)
BundlePlugin.bundleSettings(BackendRegion)
inConfig(BackendRegion)(Seq(
  normalizedName := "reactive-maps-backend-region",
  BundleKeys.endpoints := Map("akka-remote" -> Endpoint("tcp")),
  BundleKeys.roles := Set("intranet"),
  BundleKeys.startCommand :=
    Seq((BundleKeys.executableScriptPath in BackendRegion).value) ++
      (javaOptions in BackendRegion).value ++
      Seq(
        "-Dakka.cluster.roles.1=backend-region",
        "-main", "backend.Main"
      )
))

lazy val BackendSummary = config("backend-summary").extend(BackendRegion)
BundlePlugin.bundleSettings(BackendSummary)
inConfig(BackendSummary)(Seq(
  normalizedName := "reactive-maps-backend-summary",
  BundleKeys.startCommand :=
    Seq((BundleKeys.executableScriptPath in BackendSummary).value) ++
      (javaOptions in BackendSummary).value ++
      Seq(
        "-Dakka.cluster.roles.1=backend-summary",
        "-main", "backend.Main"
      )
))

// Bundle publishing configuration

inConfig(Bundle)(Seq(
  bintrayVcsUrl := Some("https://github.com/typesafehub/ReactiveMaps"),
  bintrayOrganization := Some("typesafe")
))
BintrayBundle.settings(BackendRegion)
BintrayBundle.settings(BackendSummary)


// Root project

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .configs(BackendRegion, BackendSummary)
