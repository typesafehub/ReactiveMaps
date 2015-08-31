package controllers

import javax.inject.Inject

import akka.actor.Props
import play.api.mvc._
import actors.ClientConnection
import play.api.Play.current
import actors.ClientConnection.ClientEvent

class Application @Inject() (
    clientConnectionFactory: ClientConnection.Factory
) extends Controller {

  /**
   * The index page.
   */
  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocket.acceptWithActor[ClientEvent, ClientEvent] { _ => upstream =>
    Props(clientConnectionFactory(email, upstream))
  }
}