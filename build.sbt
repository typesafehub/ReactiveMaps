import com.typesafe.sbt.jse.SbtJsTaskPlugin._
import com.typesafe.sbt.jse.SbtJsEnginePlugin._
import com.typesafe.sbt.mocha.SbtMochaPlugin._
import com.typesafe.sbt.web.SbtWebPlugin._
import com.typesafe.sbt.less.SbtLessPlugin._
import com.typesafe.sbt.coffeescript.SbtCoffeeScriptPlugin._
import WebKeys._

name := "reactive-maps"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-contrib" % "2.2.1",
  "com.typesafe.play.extras" %% "play-geojson" % "1.0.0",
  "org.webjars" %% "webjars-play" % "2.2.1",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "leaflet" % "0.7.2",
  "org.webjars" % "requirejs-node" % "2.1.11-1" % "test",
  "org.webjars" % "squirejs" % "0.1.0" % "test"
)

play.Project.playScalaSettings

// The settings below won't be needed when we upgrade to Play 2.3
webSettings

jsEngineAndTaskSettings

lessSettings

coffeeScriptSettings

mochaSettings

MochaKeys.requires += "SetupMocha.js"

JsEngineKeys.engineType in MochaKeys.mocha := JsEngineKeys.EngineType.Node

sourceDirectory in Assets := baseDirectory.value / "app" / "assets"

sourceDirectory in TestAssets := baseDirectory.value / "test" / "assets"

resourceDirectory in Assets := baseDirectory.value / "public"

public in Assets := webTarget.value / "public" / "main" / "public"

products in Compile += {
  (assets in Assets).value
  webTarget.value / "public" / "main"
}

products in Runtime += {
  (assets in Assets).value
  webTarget.value / "public" / "main"
}

playCopyAssets := {
  Seq.empty[(File, File)]
}

lessEntryPoints := baseDirectory.value * "null"

coffeescriptEntryPoints := baseDirectory.value * "null"
