package controllers

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import actors.ClientConnection
import play.api.Play.current
import actors.ClientConnection.ClientEvent

class Application @Inject() (
    clientConnectionFactory: ClientConnection.Factory
)(implicit mat: Materializer, sys: ActorSystem) extends Controller {

  /**
   * The index page.
   */
  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocket.accept(_ =>
    ActorFlow.actorRef[ClientEvent, ClientEvent] { upstream =>
      Props(clientConnectionFactory(email, upstream))
    }
  )

}