package backend

import akka.actor.Actor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.FromConfig
import models.backend.{ RegionId, RegionPoints, UserPosition }
import akka.actor.ActorLogging

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
class RegionManager extends Actor with ActorLogging {
  import RegionManager._

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")
  val settings = Settings(context.system)

  def receive = {
    case UpdateUserPosition(regionId, userPosition) =>
      val region = context.child(regionId.name).getOrElse {
        log.debug("Creating region: {}", regionId.name)
        context.actorOf(Region.props(regionId), regionId.name)
      }
      region ! userPosition

    case UpdateRegionPoints(regionId, regionPoints) =>
      val summaryRegion = context.child(regionId.name).getOrElse {
        log.debug("Creating summary region: {}", regionId.name)
        context.actorOf(SummaryRegion.props(regionId), regionId.name)
      }
      summaryRegion ! regionPoints

    case p @ RegionPoints(regionId, _) =>

      // count reported by child region, propagate it to summary region on responsible node
      settings.GeoFunctions.summaryRegionForRegion(regionId).foreach { summaryRegionId =>
        regionManagerRouter ! UpdateRegionPoints(summaryRegionId, p)
      }
  }

}