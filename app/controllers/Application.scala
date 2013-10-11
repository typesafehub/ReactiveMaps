package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{JsString, Json}
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.PositionSubscriber
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import backend.{Cluster, UserPosition, BoundingBox}
import models.geojson._
import models._

object Application extends Controller {

  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  def stream(email: String) = WebSocket.using[ClientEvent] { req ⇒

    val (enumerator, channel) = Concurrent.broadcast[ClientEvent]

    val akkaSystem = Akka.system
    val regionManagerClient = akkaSystem.actorSelection(akkaSystem / "regionManagerClient")

    val subscriber = akkaSystem.actorOf(Props {
      new PositionSubscriber(update => if (update.updates.size > 0) channel.push(
        UserPositions(FeatureCollection(
          features = update.updates.map { pos =>
            val properties = pos match {
              case _: UserPosition => Json.obj("timestamp" -> pos.timestamp)
              case Cluster(_, _, _, count) => Json.obj(
                "timestamp" -> pos.timestamp,
                "cluster" -> count
              )
            }

            Feature(
              geometry = Point(pos.position),
              id = Some(JsString(pos.id)),
              properties = Some(properties)
            )
          },
          bbox = update.area.map(area => (area.southWest, area.northEast))
        )))
      )
    })

    (Iteratee.foreach[ClientEvent] {
      case ViewingArea(area) => area.bbox.foreach { bbox =>
        subscriber ! BoundingBox(bbox._1, bbox._2)
      }
      case UserMoved(point) => regionManagerClient ! UserPosition(email, System.currentTimeMillis(), point.coordinates)
    }, enumerator.onDoneEnumerating(akkaSystem.stop(subscriber)))
  }

}