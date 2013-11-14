package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{ JsString, Json }
import play.api.libs.concurrent.Akka
import actors.{ Actors, PositionSubscriber }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.extras.geojson._
import models.client._
import models.backend._
import actors.PositionSubscriber.PositionSubscriberUpdate

object Application extends Controller {

  def system = Akka.system

  /**
   * The index page.
   */
  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocket.using[ClientEvent] { req =>

    // We use a broadcast enumerator to send back to the client.
    val (enumerator, channel) = Concurrent.broadcast[ClientEvent]

    // publish function that serialises the updates to a UserPositions events from the 
    // PositionSubscriber actor
    val publish: PositionSubscriber.ClientPublish = { update =>
      if (update.updates.nonEmpty) channel.push(convertUpdateToClientEvent(update))
    }

    // Create the subscriber actor to subscribe to position updates
    val subscriber = system.actorOf(PositionSubscriber.props(publish))

    // Iteratee to handle the events from the client side.
    val iteratee = Iteratee.foreach[ClientEvent] {

      // The users has moved their position, publish to the region
      case UserMoved(point) =>
        Actors.regionManagerClient ! UserPosition(email, System.currentTimeMillis(), point.coordinates)

      // The viewing area has changed, tell the subscriber
      case ViewingArea(area) => area.bbox.foreach { bbox =>
        subscriber ! BoundingBox(bbox._1, bbox._2)
      }
    }

    (iteratee, enumerator.onDoneEnumerating(system.stop(subscriber)))
  }

  private def convertUpdateToClientEvent(update: PositionSubscriberUpdate): UserPositions = {
    UserPositions(FeatureCollection(
      features = update.updates.map { pos =>

        val properties = pos match {
          case _: UserPosition => Json.obj("timestamp" -> pos.timestamp)
          case Cluster(_, _, _, count) => Json.obj(
            "timestamp" -> pos.timestamp,
            "count" -> count)
        }

        Feature(
          geometry = Point(pos.position),
          id = Some(JsString(pos.id)),
          properties = Some(properties))
      },
      bbox = update.area.map(area => (area.southWest, area.northEast))))
  }
}