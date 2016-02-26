// Update this when a new patch of Reactive Platform is available
val rpVersion = "15v09p04"

// Update this when a major version of Reactive Platform is available
val rpUrl = "https://repo.typesafe.com/typesafe/for-subscribers-only/AEE4D829FC38A3247F251ED25BA45ADD675D48EB"

addSbtPlugin("com.typesafe.rp" % "sbt-typesafe-rp" % rpVersion)

// The resolver name must start with typesafe-rp
resolvers += "typesafe-rp-mvn" at rpUrl

// The resolver name must start with typesafe-rp
resolvers += Resolver.url("typesafe-rp-ivy", url(rpUrl))(Resolver.ivyStylePatterns)