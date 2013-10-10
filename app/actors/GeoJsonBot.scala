package actors

import akka.actor.{ ActorRef, Actor }
import scala.concurrent.duration._
import backend.UserPosition
import java.util.UUID
import models.geojson.{LineString, LatLng}

object GeoJsonBot {
  case object Step
}

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
