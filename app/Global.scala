import actors.{GeoJsonBot, RegionManagerClient}
import akka.actor.{Props, ActorSystem}
import backend.RegionManager
import models.geojson.{LineString, FeatureCollection, LatLng}
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.{Logger, Play, Application, GlobalSettings}
import scalax.io.Resource

object Global extends GlobalSettings {
  override def onStart(app: Application) = {

    val akkaSystem = Akka.system(app)
    akkaSystem.actorOf(Props[RegionManager], "regionManager")
    val regionManagerClient = akkaSystem.actorOf(Props[RegionManagerClient], "regionManagerClient")

    // Find all the bots and start them
    def startBot(id: Int): Unit = {
      app.resource("bots/" + id + ".json").foreach { url =>
        val json = Json.parse(Resource.fromURL(url).string)
        Json.fromJson[FeatureCollection[LatLng]](json).fold(
          invalid => Logger.error("Error loading geojson bot: " + invalid),
          valid => valid.features.zipWithIndex.map { feature =>
            feature._1.geometry match {
              case route: LineString[LatLng] =>
                val userId = "bot-" + id + "-" + feature._1.id.getOrElse(feature._2) + "-" + feature._1.properties.flatMap(js => (js \ "name").asOpt[String]).getOrElse("")
                akkaSystem.actorOf(Props(new GeoJsonBot(route, userId, regionManagerClient)))
              case other =>
            }
          }
        )
        startBot(id + 1)
      }
    }

    startBot(1)
  }
}
