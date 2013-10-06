package controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Iteratee }
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.{ RegionManagerClient, PositionSubscriber, MockGpsBot }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import akka.actor.ActorSystem
import akka.cluster.Cluster
import backend.Area
import backend.Position
import akka.actor.ActorRef
import backend.RegionManager

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def stream = WebSocket.using[JsValue] { req ⇒
    akkaSystem.actorOf(Props(classOf[MockGpsBot], regionManagerClient))
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val subscriber = Akka.system.actorOf(Props {
      new PositionSubscriber(userPosition => channel.push(Json.arr(userPosition.position.lat, userPosition.position.lon)))
    })
    subscriber ! Area(Position(-36.0, 149.0), Position(-33.0, 152.0))
    (Iteratee.ignore[JsValue], enumerator.onDoneEnumerating(akkaSystem.stop(subscriber)))
  }

  lazy val regionManagerClient: ActorRef =
    akkaSystem.actorOf(Props[RegionManagerClient], "regionManagerClient")

  lazy val akkaSystem: ActorSystem = {
    val akkaSystem = Akka.system
    // FIXME regionManager should only be started in backend
    akkaSystem.actorOf(Props[RegionManager], "regionManager")
    akkaSystem
  }

}