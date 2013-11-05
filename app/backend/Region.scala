package backend

import scala.concurrent.duration.Deadline

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import backend.RegionManager.Subscribe
import backend.RegionManager.Unsubscribe
import models.backend.{ RegionId, RegionPoints, BoundingBox, UserPosition }

object Region {

  def props(regionId: RegionId): Props = Props(classOf[Region], regionId)

  private case object Tick
}

/**
 * A region.
 *
 * These sit at the lowest level, and hold all the users in that region, and publish their summaries up.
 */
class Region(regionId: RegionId) extends Actor with ActorLogging {
  import Region._

  val settings = Settings(context.system)

  val regionBounds: BoundingBox = settings.GeoFunctions.boundingBoxForRegion(regionId)
  var activeUsers = Map.empty[String, (UserPosition, Deadline)]

  var subscribers = Set.empty[ActorRef]

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
      subscribers.foreach(_ ! p)

    case Subscribe(_, subscriber) =>
      subscribers += subscriber
      context.watch(subscriber)

    case Unsubscribe(_, subscriber) =>
      subscribers -= subscriber
      context.unwatch(subscriber)

    case Terminated(ref) =>
      subscribers -= ref

    case Tick =>
      val obsolete = activeUsers.collect {
        case (userId, (position, deadline)) if deadline.isOverdue() => userId
      }
      activeUsers --= obsolete

      // Cluster
      val points = RegionPoints(regionId, settings.GeoFunctions.cluster(regionId.name, regionBounds,
        activeUsers.collect { case (_, (position, _)) => position }(collection.breakOut)))

      // propagate the points to the summary region via the manager
      context.parent ! points

  }

}
