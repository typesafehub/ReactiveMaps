package models

import models.geojson._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.WebSocket.FrameFormatter

/**
 * Events to/from the client side
 */
sealed trait ClientEvent

object ClientEvent {
  implicit def clientEventFormat: Format[ClientEvent] = Format(
    Reads { json =>
      (json \ "event").as[String] match {
        case "user-positions" => UserPositions.userPositionsFormat.reads(json)
        case "viewing-area" => ViewingArea.viewingAreaFormat.reads(json)
        case "user-moved" => UserMoved.userMovedFormat.reads(json)
        case other => JsError("Unknown client event: " + other)
      }
    },
    Writes {
      case up: UserPositions => UserPositions.userPositionsFormat.writes(up)
      case va: ViewingArea => ViewingArea.viewingAreaFormat.writes(va)
      case um: UserMoved => UserMoved.userMovedFormat.writes(um)
    }
  )

  implicit def clientEventFrameFormatter: FrameFormatter[ClientEvent] = FrameFormatter.jsonFrame.transform(
    clientEvent => Json.toJson(clientEvent),
    json => Json.fromJson[ClientEvent](json).fold(
      invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
      valid => valid
    )
  )
}

/**
 * Event sent to the client when one or more users have updated their position in the current area
 */
case class UserPositions(positions: FeatureCollection[LatLng]) extends ClientEvent

object UserPositions {
  implicit def userPositionsFormat: Format[UserPositions] = (
    (__ \ "event").format[String] ~
    (__ \ "positions").format[FeatureCollection[LatLng]]
  ).apply({
    case ("user-positions", positions) => UserPositions(positions)
  }, userPositions => ("user-positions", userPositions.positions))
}

/**
 * Event sent from the client when the viewing area has changed
 */
case class ViewingArea(area: Polygon[LatLng]) extends ClientEvent

object ViewingArea {
  implicit def viewingAreaFormat: Format[ViewingArea] = (
    (__ \ "event").format[String] ~
    (__ \ "area").format[Polygon[LatLng]]
  ).apply({
    case ("viewing-area", area) => ViewingArea(area)
  }, viewingArea => ("viewing-area", viewingArea.area))
}

/**
 * Event sent from the client when they have moved
 */
case class UserMoved(position: Point[LatLng]) extends ClientEvent

object UserMoved {
  implicit def userMovedFormat: Format[UserMoved] = (
    (__ \ "event").format[String] ~
    (__ \ "position").format[Point[LatLng]]
  ).apply({
    case ("user-moved", position) => UserMoved(position)
  }, userMoved => ("user-moved", userMoved.position))
}
