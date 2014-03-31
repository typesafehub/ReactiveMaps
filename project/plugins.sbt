// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-less-plugin" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript-plugin" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha-plugin" % "1.0.0-SNAPSHOT")
