package backend

import scala.concurrent.duration.Deadline

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import backend.RegionManager.Subscribe
import backend.RegionManager.Unsubscribe
import models.backend.{ RegionId, RegionPoints, BoundingBox, PointOfInterest }

object SummaryRegion {

  def props(regionId: RegionId): Props = Props(classOf[SummaryRegion], regionId)

  private case object Tick
}

/**
 * A summary region.
 *
 * Summary regions receive region points from their 4 sub regions, cluster them, and broadcast the resulting points.
 */
class SummaryRegion(regionId: RegionId) extends Actor with ActorLogging {
  import SummaryRegion._

  val settings = Settings(context.system)

  /**
   * The bounding box for this region.
   */
  val regionBounds: BoundingBox = settings.GeoFunctions.boundingBoxForRegion(regionId)

  /**
   * The active points for this region, keyed by sub region id.
   *
   * The values are the points for the sub region, tupled with the deadline they are valid until.
   */
  var activePoints = Map.empty[RegionId, (Seq[PointOfInterest], Deadline)]

  var subscribers = Set.empty[ActorRef]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(settings.SummaryInterval / 2, settings.SummaryInterval, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
    log.debug("Stopped summary region: {}", regionId.name)
  }

  def receive = {
    case RegionPoints(id, points) =>
      activePoints += id -> (points, Deadline.now + settings.ExpiryInterval)

    case Subscribe(_, subscriber) =>
      subscribers += subscriber
      context.watch(subscriber)

    case Unsubscribe(_, subscriber) =>
      subscribers -= subscriber
      context.unwatch(subscriber)

    case Terminated(ref) =>
      subscribers -= ref

    case Tick =>
      // Expire old ones
      val obsolete = activePoints.collect {
        case (rid, (map, deadline)) if deadline.isOverdue() => rid
      }
      activePoints --= obsolete

      // Cluster
      val points = RegionPoints(regionId, settings.GeoFunctions.cluster(regionId.name, regionBounds,
        activePoints.values.flatMap(_._1)(collection.breakOut)))

      // propagate the points to higher level summary region via the manager
      context.parent ! points
      // publish total count to subscribers
      subscribers.foreach(_ ! points)
  }

}
