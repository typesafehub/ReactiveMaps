package backend

import akka.actor.Actor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.FromConfig
import models.backend.{ RegionId, RegionPoints, UserPosition }

object RegionManager {

  def props(): Props = Props[RegionManager]

  case class UpdateUserPosition(regionId: RegionId, userPosition: UserPosition) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId
  }

  case class UpdateRegionPoints(regionId: RegionId, regionPoints: RegionPoints) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId
  }

}

/**
 * Handles instantiating region and summary region actors when data arrives for them, if they don't already exist.
 */
class RegionManager extends Actor {
  import RegionManager._

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")
  val settings = Settings(context.system)

  def receive = {
    case UpdateUserPosition(regionId, userPosition) =>
      context.child(regionId.name) match {
        case Some(ref) => ref ! userPosition
        case None      => context.actorOf(Region.props(regionId), regionId.name) ! userPosition
      }

    case UpdateRegionPoints(regionId, regionPoints) =>
      context.child(regionId.name) match {
        case Some(ref) => ref ! regionPoints
        case None      => context.actorOf(SummaryRegion.props(regionId), regionId.name) ! regionPoints
      }

    case p @ RegionPoints(regionId, _) =>

      // count reported by child region, propagate it to summary region on responsible node
      settings.GeoFunctions.summaryRegionForRegion(regionId).foreach { summaryRegionId =>
        regionManagerRouter ! UpdateRegionPoints(summaryRegionId, p)
      }
  }

}