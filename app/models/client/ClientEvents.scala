package models.client

import play.extras.geojson._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

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
 * JSON serialisers/deserialisers
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
