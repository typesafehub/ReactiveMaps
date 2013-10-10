package actors

import akka.actor.Actor
import backend.UserPosition
import akka.actor.Props
import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import backend.RegionManager.UpdateUserPosition
import akka.routing.FromConfig
import models.geojson.LatLng

class RegionManagerClient extends Actor {

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

  def receive = {
    case p: UserPosition =>
      regionManagerRouter ! UpdateUserPosition(region(p.position), p)
  }

  // FIXME
  def region(pos: LatLng): String = "dummyRegion1"
}