package actors

import akka.actor.Actor
import backend.{RegionId, UserPosition}
import akka.actor.Props
import backend.RegionManager.UpdateUserPosition
import akka.routing.FromConfig

class RegionManagerClient extends Actor {

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

  def receive = {
    case p: UserPosition =>
      regionManagerRouter ! UpdateUserPosition(RegionId(p.position), p)
  }
}