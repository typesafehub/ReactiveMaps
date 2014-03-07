package backend

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.actor.{ ActorRef, Props }
import play.api.libs.json.Json
import scalax.io.Resource
import play.extras.geojson.{ LineString, LatLng, FeatureCollection }
import play.api.Logger
import actors.GeoJsonBot
import java.net.URL
import akka.actor.Actor

object BotManager {
  def props(regionManagerClient: ActorRef, data: Seq[URL]): Props =
    Props(new BotManager(regionManagerClient, data))

  private case object Tick
}

/**
 * Loads and starts GeoJSON bots
 */
class BotManager(regionManagerClient: ActorRef, data: Seq[URL]) extends Actor {
  import BotManager._

  var total = 0
  val max = Settings(context.system).TotalNumberOfBots

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(1.seconds, 3.seconds, self, Tick)

  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case Tick if total >= max =>
      tickTask.cancel()

    case Tick =>
      val totalBefore = total
      val originalTrail = total == 0
      data.zipWithIndex.foreach {
        case (url, id) =>
          val json = Json.parse(Resource.fromURL(url).string)
          Json.fromJson[FeatureCollection[LatLng]](json).fold(
            { invalid =>
              Logger.error("Error loading geojson bot: " + invalid)
            }, valid => valid.features.zipWithIndex.map { feature =>
              feature._1.geometry match {
                case route: LineString[LatLng] if total < max =>
                  total += 1
                  val userId = "bot-" + total + "-" + ThreadLocalRandom.current.nextInt(1000) + "-" + id + "-" + feature._1.id.getOrElse(feature._2) + "-" + feature._1.properties.flatMap(js => (js \ "name").asOpt[String]).getOrElse("")
                  val offset =
                    if (originalTrail) (0.0, 0.0)
                    else (ThreadLocalRandom.current.nextDouble() * 15.0,
                      ThreadLocalRandom.current.nextDouble() * -30.0)
                  context.actorOf(GeoJsonBot.props(route, offset, userId, regionManagerClient))
                case other =>
              }
            })
      }

      println("Started " + (total - totalBefore) + " bots, total " + total)
  }
}
