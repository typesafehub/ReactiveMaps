package actors

import akka.actor.{ ActorRef, Actor }
import scala.concurrent.duration._
import play.extras.geojson.{ LineString, LatLng }
import models.backend.UserPosition
import akka.actor.Props
import models.backend.BoundingBox
import scala.concurrent.forkjoin.ThreadLocalRandom
import actors.PositionSubscriber.PositionSubscriberUpdate
import backend.UserMetaData.UpdateUserPosition

object GeoJsonBot {
  def props(trail: LineString[LatLng], offset: (Double, Double), userId: String, regionManagerClient: ActorRef, userMetaData: ActorRef): Props =
    Props(new GeoJsonBot(trail, offset, userId, regionManagerClient, userMetaData))

  private case object Step
  private case object Zoom
}

/**
 * A bot that walks back and forth along a GeoJSON LineString.
 */
class GeoJsonBot(trail: LineString[LatLng], offset: (Double, Double), userId: String,
                 regionManagerClient: ActorRef, userMetaData: ActorRef) extends Actor {

  import GeoJsonBot._

  val (latOffset, lngOffset) = offset
  var pos = 0
  var direction = -1
  var stepCount = 0

  import context.dispatcher
  val stepTask = context.system.scheduler.schedule(1 second, 1 second, context.self, Step)

  val positionSubscriber: ActorRef = context.actorOf(PositionSubscriber.props(self))

  override def postStop(): Unit = {
    stepTask.cancel()
  }

  def receive = {
    case Step =>
      if (pos == trail.coordinates.size - 1 || pos == 0) {
        direction = -direction
      }
      pos += direction
      val c = trail.coordinates(pos)
      val userPos = UserPosition(userId, System.currentTimeMillis, LatLng(c.lat + latOffset, c.lng + lngOffset))
      regionManagerClient ! userPos
      userMetaData ! UpdateUserPosition(userId, userPos.position)

      stepCount += 1
      if (stepCount % 30 == 0) {
        val w = ThreadLocalRandom.current.nextDouble() * 10.0
        val h = ThreadLocalRandom.current.nextDouble() * 20.0
        val southWest = LatLng(c.lat + latOffset - w / 2, c.lng + lngOffset - h / 2)
        val northEast = LatLng(c.lat + latOffset + w / 2, c.lng + lngOffset + h / 2)
        positionSubscriber ! BoundingBox(southWest, northEast)
      }

    case _: PositionSubscriberUpdate =>
    // Ignore

  }
}
