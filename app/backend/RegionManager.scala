package backend

import akka.actor.Actor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.FromConfig

object RegionManager {
  case class UpdateUserPosition(regionId: String, userPosition: UserPosition) extends ConsistentHashable {
    override def consistentHashKey: Any = regionId
  }

  case class UpdateRegionCount(summaryRegionId: String, regionCount: RegionCount) extends ConsistentHashable {
    override def consistentHashKey: Any = summaryRegionId
  }

}

class RegionManager extends Actor {
  import RegionManager._

  val regionManagerRouter = context.actorOf(Props.empty.withRouter(FromConfig), "router")

  def receive = {
    case UpdateUserPosition(regionId, userPosition) =>
      context.child(regionId) match {
        case Some(ref) => ref ! userPosition
        case None      => context.actorOf(Props[Region], regionId) ! userPosition
      }

    case UpdateRegionCount(summaryRegionId, regionCount) =>
      context.child(summaryRegionId) match {
        case Some(ref) => ref ! regionCount
        case None      => context.actorOf(Props[SummaryRegion], summaryRegionId) ! regionCount
      }

    case c @ RegionCount(regionId, _) =>
      // count reported by child region, propagate it to summary region on responsible node 
      summaryRegion(regionId) foreach { summaryRegionId =>
        regionManagerRouter ! UpdateRegionCount(summaryRegionId, c)
      }

  }

  // FIXME
  def summaryRegion(regionId: String): Option[String] =
    if (regionId == "dummyRegion1") Some("dummySummaryRegion1")
    else if (regionId == "dummySummaryRegion1") Some("dummySummaryRegion2")
    else None

}