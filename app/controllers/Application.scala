package controllers

import play.api.mvc._
import actors.{ WebSocketActor, ClientConnection, Actors }
import play.api.Play.current
import actors.ClientConnection.ClientEvent

object Application extends Controller {

  /**
   * The index page.
   */
  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  /**
   * The WebSocket
   */
  def stream(email: String) = WebSocketActor.actorOf[ClientEvent] { _ =>
    upstream =>
      ClientConnection.props(email, upstream, Actors.regionManagerClient, Actors.userMetaData)
  }
}