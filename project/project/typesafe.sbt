addSbtPlugin("com.typesafe.rp" % "sbt-typesafe-rp" % "15v09p01")

val typesafeUrl = "https://repo.typesafe.com/typesafe/for-subscribers-only/AEE4D829FC38A3247F251ED25BA45ADD675D48EB"

resolvers += "typesafe-rp-mvn" at typesafeUrl

resolvers += Resolver.url("typesafe-rp-ivy", url(typesafeUrl))(Resolver.ivyStylePatterns)

resolvers += Resolver.typesafeRepo("releases")
