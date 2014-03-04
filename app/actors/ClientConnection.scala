package actors

import akka.actor.{Props, ActorRef, Actor}
import play.extras.geojson._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter
import actors.PositionSubscriber.PositionSubscriberUpdate
import models.backend._

object ClientConnection {

  def props(email: String, upstream: ActorRef, regionManagerClient: ActorRef): Props = {
    Props(new ClientConnection(email, upstream, regionManagerClient))
  }

  /**
   * Events to/from the client side
   */
  sealed trait ClientEvent

  /**
   * Event sent to the client when one or more users have updated their position in the current area
   */
  case class UserPositions(positions: FeatureCollection[LatLng]) extends ClientEvent

  /**
   * Event sent from the client when the viewing area has changed
   */
  case class ViewingArea(area: Polygon[LatLng]) extends ClientEvent

  /**
   * Event sent from the client when they have moved
   */
  case class UserMoved(position: Point[LatLng]) extends ClientEvent


  /*
   * JSON serialisers/deserialisers for the above messages
   */

  object ClientEvent {
    implicit def clientEventFormat: Format[ClientEvent] = Format(
      (__ \ "event").read[String].flatMap {
        case "user-positions" => UserPositions.userPositionsFormat.map(identity)
        case "viewing-area" => ViewingArea.viewingAreaFormat.map(identity)
        case "user-moved" => UserMoved.userMovedFormat.map(identity)
        case other => Reads(_ => JsError("Unknown client event: " + other))
      },
      Writes {
        case up: UserPositions => UserPositions.userPositionsFormat.writes(up)
        case va: ViewingArea => ViewingArea.viewingAreaFormat.writes(va)
        case um: UserMoved => UserMoved.userMovedFormat.writes(um)
      }
    )

    /**
     * Formats WebSocket frames to be ClientEvents.
     */
    implicit def clientEventFrameFormatter: FrameFormatter[ClientEvent] = FrameFormatter.jsonFrame.transform(
      clientEvent => Json.toJson(clientEvent),
      json => Json.fromJson[ClientEvent](json).fold(
        invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
        valid => valid
      )
    )
  }

  object UserPositions {
    implicit def userPositionsFormat: Format[UserPositions] = (
      (__ \ "event").format[String] ~
        (__ \ "positions").format[FeatureCollection[LatLng]]
      ).apply({
      case ("user-positions", positions) => UserPositions(positions)
    }, userPositions => ("user-positions", userPositions.positions))
  }

  object ViewingArea {
    implicit def viewingAreaFormat: Format[ViewingArea] = (
      (__ \ "event").format[String] ~
        (__ \ "area").format[Polygon[LatLng]]
      ).apply({
      case ("viewing-area", area) => ViewingArea(area)
    }, viewingArea => ("viewing-area", viewingArea.area))
  }

  object UserMoved {
    implicit def userMovedFormat: Format[UserMoved] = (
      (__ \ "event").format[String] ~
        (__ \ "position").format[Point[LatLng]]
      ).apply({
      case ("user-moved", position) => UserMoved(position)
    }, userMoved => ("user-moved", userMoved.position))
  }

}

/**
 * Represents a client connection
 *
 * @param email The email address of the client
 * @param regionManagerClient The region manager client to send updates to
 */
class ClientConnection(email: String, upstream: ActorRef, regionManagerClient: ActorRef) extends Actor {

  // Create the subscriber actor to subscribe to position updates
  val subscriber = context.actorOf(PositionSubscriber.props(self), "positionSubscriber")

  import ClientConnection._

  def receive = {
    // The users has moved their position, publish to the region
    case UserMoved(point) =>
      regionManagerClient ! UserPosition(email, System.currentTimeMillis(), point.coordinates)

    // The viewing area has changed, tell the subscriber
    case ViewingArea(area) => area.bbox.foreach { bbox =>
      subscriber ! BoundingBox(bbox._1, bbox._2)
    }

    // The subscriber received an update
    case PositionSubscriberUpdate(area, updates) =>
      val userPositions = UserPositions(FeatureCollection(
        features = updates.map { pos =>

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
        bbox = area.map(area => (area.southWest, area.northEast))
      ))

      upstream ! userPositions

  }
}
