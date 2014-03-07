package controllers

import akka.pattern.{ AskTimeoutException, ask }
import akka.util.Timeout

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.Play.current

import scala.concurrent.duration._

import actors.Actors
import backend.UserMetaData._

object UserController extends Controller {

  implicit val userWrites = Json.writes[User]

  def get(id: String) = Action.async {
    implicit val timeout = Timeout(2.seconds)

    (Actors.userMetaData ? GetUser(id))
      .mapTo[User]
      .map { user =>
        Ok(Json.toJson(user))
      } recover {
        case _: AskTimeoutException => NotFound
      }
  }
}