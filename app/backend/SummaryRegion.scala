package backend

import scala.concurrent.duration.Deadline
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import models.backend.{ RegionId, RegionPoints, BoundingBox, PointOfInterest }
import akka.actor.Props
import akka.actor.ActorLogging

object SummaryRegion {

  def props(regionId: RegionId): Props = Props(new SummaryRegion(regionId))

  private case object Tick
}

/**
 * Summary regions receive region points from their 4 sub regions, cluster them, and publishes the resulting points
 * to subscribers of the topic with the region id.
 */
class SummaryRegion(regionId: RegionId) extends Actor with ActorLogging {
  import SummaryRegion._

  val mediator = DistributedPubSubExtension(context.system).mediator
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

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(settings.SummaryInterval / 2, settings.SummaryInterval, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
    log.debug("Stopped summary region: {}", regionId.name)
  }

  def receive = {
    case RegionPoints(id, points) =>
      // update from sub-region
      activePoints += id -> (points, Deadline.now + settings.ExpiryInterval)

    case Tick =>
      // expire inactive sub-regions
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
      mediator ! Publish(regionId.name, points)

      // stop the actor when no active sub-regions
      if (activePoints.isEmpty)
        context.stop(self)
  }

}
