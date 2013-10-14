package actors

import akka.actor.{ ActorRef, Actor }
import scala.concurrent.duration._
import models.geojson.{LineString, LatLng}
import models.backend.UserPosition

object GeoJsonBot {
  case object Step
}

/**
 * A bot that walks back and forth along a GeoJSON LineString.
 */
class GeoJsonBot(trail: LineString[LatLng], userId: String, regionManagerClient: ActorRef) extends Actor {

  import GeoJsonBot._

  var pos = 0
  var direction = -1

  import context.dispatcher
  val stepTask = context.system.scheduler.schedule(1 second, 1 second, context.self, Step)

  override def postStop(): Unit = stepTask.cancel()

  def receive = {
    case Step =>
      if (pos == trail.coordinates.size - 1 || pos == 0) {
        direction = -direction
      }
      pos += direction
      val userPos = UserPosition(userId, System.currentTimeMillis, trail.coordinates(pos))
      regionManagerClient ! userPos
  }
}
