package controllers

import play.api.mvc._
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.{Client, MockGpsBot}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def stream = WebSocket.using[JsValue] { req =>
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val clientActor = Akka.system.actorOf(Props {
      new Client(bot, pos => channel.push(Json.arr(pos.lat, pos.lon)))
    })
    (Iteratee.ignore[JsValue], enumerator.onDoneEnumerating(Akka.system.stop(clientActor)))
  }

  lazy val bot = Akka.system.actorOf(Props[MockGpsBot])

}