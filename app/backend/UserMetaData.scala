package backend

import akka.actor.{ Props, Actor }
import play.extras.geojson.LatLng

object UserMetaData {
  case class GetUser(id: String)
  case class User(id: String, distance: Double)
  case class UpdateUserPosition(id: String, position: LatLng)

  val props = Props[UserMetaData]
}

class UserMetaData extends Actor {

  import UserMetaData._

  val settings = Settings(context.system)

  var users = Map.empty[String, (LatLng, Double)]

  def receive = {
    case GetUser(id) =>
      users.get(id) match {
        case Some((_, distance)) =>
          sender ! User(id, distance)
        case None => sender ! User(id, 0)
      }

    case UpdateUserPosition(id, position) =>
      val distance = users.get(id) match {
        case Some((lastPosition, lastDistance)) =>
          lastDistance + settings.GeoFunctions.distanceBetweenPoints(lastPosition, position)
        case None => 0
      }

      users += (id -> (position, distance))
  }
}