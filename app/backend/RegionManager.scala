package backend

import akka.actor.Actor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.FromConfig
import models.backend.{ RegionId, RegionPoints, UserPosition }
import akka.actor.ActorLogging

object RegionManager {

  def props(): Props = Props[RegionManager]

  /**
   * Update the users position.
   *
   * Sent by clients of the backend when they want to update a users position.
   *
   * @param regionId The region id that position is in.  This is used as the hash key for deciding which node
   *                 to route the update to.
   * @param userPosition The user position object.
   */
  case class UpdateUserPosition(regionId: RegionId, userPosition: UserPosition) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId.name
  }

  /**
   * Update the region points at a given region.
   *
   * Sent by child regions to update their data in their parent summary region.
   *
   * @param regionId The region id that position is in.  This is used as the hash key for deciding which node
   *                 to route the update to.
   * @param regionPoints The points to update.
   */
  case class UpdateRegionPoints(regionId: RegionId, regionPoints: RegionPoints) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId.name
  }

}

/**
 * Handles instantiating region and summary region actors when data arrives for them, if they don't already exist.
 * It also routes the `RegionPoints` from child `Region` or `SummaryRegion` to the node 
 * responsible for the target region.
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