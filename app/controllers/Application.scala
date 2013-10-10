package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{JsString, Json}
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.PositionSubscriber
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import backend.{UserPosition, BoundingBox}
import models.geojson._
import models._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def stream(email: String) = WebSocket.using[ClientEvent] { req ⇒

    val (enumerator, channel) = Concurrent.broadcast[ClientEvent]

    val akkaSystem = Akka.system
    val regionManagerClient = akkaSystem.actorSelection(akkaSystem / "regionManagerClient")

    val subscriber = akkaSystem.actorOf(Props {
      new PositionSubscriber(update => channel.push(
        UserPositions(FeatureCollection(
          features = update.updates.map { pos =>
            Feature(
              geometry = Point(pos.position),
              id = Some(JsString(pos.userId)),
              properties = Some(Json.obj("timestamp" -> pos.timestamp))
            )
          },
          bbox = update.area.map(area => (area.a, area.b))
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