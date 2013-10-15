package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{ JsString, Json }
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.PositionSubscriber
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import models.geojson._
import models.client._
import models.backend._

object Application extends Controller {

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

    /**
     * We use a broadcast enumerator to send back to the client.
     */
    val (enumerator, channel) = Concurrent.broadcast[ClientEvent]

    val akkaSystem = Akka.system
    val regionManagerClient = akkaSystem.actorSelection(akkaSystem / "regionManagerClient")

    // publish function that serialises the updates to a UserPositions events from the 
    // PositionSubscriber actor
    val publish: PositionSubscriber.ClientPublish =
      (update => if (update.updates.size > 0) channel.push(
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
          bbox = update.area.map(area => (area.southWest, area.northEast))))))

    val subscriber = akkaSystem.actorOf(PositionSubscriber.props(publish))

    // Iteratee to handle the events from the client side.
    (Iteratee.foreach[ClientEvent] {
      case ViewingArea(area) => area.bbox.foreach { bbox =>
        subscriber ! BoundingBox(bbox._1, bbox._2)
      }
      case UserMoved(point) => regionManagerClient ! UserPosition(email, System.currentTimeMillis(), point.coordinates)
    }, enumerator.onDoneEnumerating(akkaSystem.stop(subscriber)))
  }

}