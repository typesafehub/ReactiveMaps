package backend

import scala.collection.immutable.Seq
import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.libs.json.Json
import scalax.io.Resource
import models.geojson.{LineString, LatLng, FeatureCollection}
import play.api.Logger
import actors.GeoJsonBot
import java.net.URL

class BotManager(system: ActorSystem, regionManagerClient: ActorRef, data: Seq[URL]) {

  def start() = {
    val bots = data.zipWithIndex.foldLeft(0) { (count, urlIdx) =>
      val (url, id) = urlIdx
      val json = Json.parse(Resource.fromURL(url).string)
      Json.fromJson[FeatureCollection[LatLng]](json).fold(
        { invalid =>
          Logger.error("Error loading geojson bot: " + invalid)
          count
        }, valid => valid.features.zipWithIndex.map { feature =>
          feature._1.geometry match {
            case route: LineString[LatLng] =>
              val userId = "bot-" + id + "-" + feature._1.id.getOrElse(feature._2) + "-" + feature._1.properties.flatMap(js => (js \ "name").asOpt[String]).getOrElse("")
              system.actorOf(Props(new GeoJsonBot(route, userId, regionManagerClient)))
            case other =>
          }
        }.size + count
      )
    }
    println("Started " + bots + "bots")
  }
}
