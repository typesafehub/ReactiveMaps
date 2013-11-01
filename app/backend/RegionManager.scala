package backend

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import models.backend.{ RegionId, RegionPoints, UserPosition }

object RegionManager {

  def props(): Props = Props[RegionManager]

  case class Subscribe(regionId: RegionId, subscriber: ActorRef)
  case class Unsubscribe(regionId: RegionId, subscriber: ActorRef)

}

/**
 * Handles instantiating region and summary region actors when data arrives for them, if they don't already exist.
 */
class RegionManager extends Actor with ActorLogging {
  import RegionManager._

  val settings = Settings(context.system)

  def receive = {
    case p: UserPosition =>
      val regionId = settings.GeoFunctions.regionForPoint(p.position)
      getRegion(regionId) ! p

    case p @ RegionPoints(regionId, _) =>
      // count reported by child region, propagate it to summary region
      settings.GeoFunctions.summaryRegionForRegion(regionId).foreach { summaryRegionId =>
        getSummaryRegion(summaryRegionId) ! p
      }

    case s @ Subscribe(regionId, _) =>
      if (regionId.zoomLevel == settings.MaxZoomDepth)
        getRegion(regionId) ! s
      else
        getSummaryRegion(regionId) ! s

    case u @ Unsubscribe(regionId, _) =>
      if (regionId.zoomLevel == settings.MaxZoomDepth)
        getRegion(regionId) ! u
      else
        getSummaryRegion(regionId) ! u

  }

  def getRegion(regionId: RegionId): ActorRef =
    context.child(regionId.name).getOrElse {
      log.debug("Creating region: {}", regionId.name)
      context.actorOf(Region.props(regionId), regionId.name)
    }

  def getSummaryRegion(summaryRegionId: RegionId): ActorRef =
    context.child(summaryRegionId.name).getOrElse {
      log.debug("Creating summary region: {}", summaryRegionId.name)
      context.actorOf(SummaryRegion.props(summaryRegionId), summaryRegionId.name)
    }

}