import actors.{GeoJsonBot, RegionManagerClient}
import akka.actor.{Props, ActorSystem}
import backend.{BotManager, RegionManager}
import java.net.URL
import models.geojson.{LineString, FeatureCollection, LatLng}
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.{Logger, Play, Application, GlobalSettings}
import scalax.io.Resource

object Global extends GlobalSettings {
  override def onStart(app: Application) = {

    val system = Akka.system(app)
    system.actorOf(Props[RegionManager], "regionManager")
    val regionManagerClient = system.actorOf(Props[RegionManagerClient], "regionManagerClient")

    if (app.configuration.getBoolean("bots.enabled").getOrElse(true)) {
      def findUrls(id: Int): List[URL] = {
        val url = app.resource("bots/" + id + ".json")
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }
      new BotManager(system, regionManagerClient, findUrls(1)).start()
    }
  }
}
