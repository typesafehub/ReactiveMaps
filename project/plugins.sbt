import sbt.Defaults.sbtPluginExtra

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
libraryDependencies += sbtPluginExtra(
  TypesafeLibrary.playSbtPlugin.value,
  (sbtBinaryVersion in update).value,
  (scalaBinaryVersion in update).value
)

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-bintray-bundle" % "1.2.0")
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.2.9")
