package backend

import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import scala.concurrent.duration.Deadline
import models.backend.{ RegionId, RegionPoints, BoundingBox, UserPosition }
import akka.actor.Props
import akka.actor.ActorLogging

object Region {

  def props(regionId: RegionId): Props = Props(new Region(regionId))

  private case object Tick
}

/**
 * These sit at the lowest level, and hold all the users in that region, and publish their summaries up.
 * User position updates are published to subscribers of the topic with the region id.
 */
class Region(regionId: RegionId) extends Actor with ActorLogging {
  import Region._

  val mediator = DistributedPubSubExtension(context.system).mediator
  val settings = Settings(context.system)

  val regionBounds: BoundingBox = settings.GeoFunctions.boundingBoxForRegion(regionId)
  var activeUsers = Map.empty[String, (UserPosition, Deadline)]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(settings.SummaryInterval / 2, settings.SummaryInterval, self, Tick)

  override def postStop(): Unit = {
    tickTask.cancel()
    log.debug("Stopped region: {}", regionId.name)
  }

  def receive = {
    case p @ UserPosition(userId, _, _) =>
      activeUsers += (userId -> (p, Deadline.now + settings.ExpiryInterval))
      // publish new user position to subscribers
      mediator ! Publish(regionId.name, p)

    case Tick =>
      // expire inactive users
      val obsolete = activeUsers.collect {
        case (userId, (position, deadline)) if deadline.isOverdue() => userId
      }
      activeUsers --= obsolete

      // Cluster
      val points = RegionPoints(regionId, settings.GeoFunctions.cluster(regionId.name, regionBounds,
        activeUsers.collect { case (_, (position, _)) => position }(collection.breakOut)))

      // propagate the points to the summary region via the parent manager
      context.parent ! points

      // stop the actor when no active users
      if (activeUsers.isEmpty)
        context.stop(self)

  }

}
