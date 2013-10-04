package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.{ Area, Position, PositionSubscriber, MockGpsBot }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import akka.actor.ActorSystem
import akka.cluster.Cluster
import actors.Position

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def stream = WebSocket.using[JsValue] { req ⇒
    akkaSystem.actorOf(Props[MockGpsBot])
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val subscriber = Akka.system.actorOf(Props {
      new PositionSubscriber(userPosition => channel.push(Json.arr(userPosition.position.lat, userPosition.position.lon)))
    })
    subscriber ! Area(Position(-36.0, 149.0), Position(-33.0, 152.0))
    (Iteratee.ignore[JsValue], enumerator.onDoneEnumerating(akkaSystem.stop(subscriber)))
  }

  lazy val akkaSystem: ActorSystem = {
    val akkaSystem = Akka.system
    val cluster = Cluster(akkaSystem)
    cluster.join(cluster.selfAddress)
    akkaSystem
  }

}