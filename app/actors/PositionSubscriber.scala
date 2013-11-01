package actors

import scala.collection.immutable.Seq

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import backend.RegionManager.Subscribe
import backend.RegionManager.Unsubscribe
import backend.Settings
import models.backend._

object PositionSubscriber {

  def props(regionManager: ActorRef, publish: PositionSubscriber.ClientPublish): Props =
    Props(new PositionSubscriber(regionManager, publish))

  case class PositionSubscriberUpdate(area: Option[BoundingBox], updates: Seq[PointOfInterest])

  type ClientPublish = PositionSubscriberUpdate => Unit

  private case object Tick
}

/**
 * A subscriber to position data.
 *
 * @param publish The function to publish position updates to.
 */
class PositionSubscriber(regionManager: ActorRef, publish: PositionSubscriber.ClientPublish) extends Actor with ActorLogging {
  import PositionSubscriber._

  val settings = Settings(context.system)

  /**
   * The current regions subscribed to
   */
  var regions = Set.empty[RegionId]

  /**
   * The current bounding box subscribed to
   */
  var currentArea: Option[BoundingBox] = None

  /**
   * The unpublished position updates
   */
  var updates: Map[String, PointOfInterest] = Map.empty

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(settings.SubscriberBatchInterval, settings.SubscriberBatchInterval,
    self, Tick)

  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case bbox: BoundingBox =>
      // Calculate new regions
      val newRegions = settings.GeoFunctions.regionsForBoundingBox(bbox)
      // Subscribe to any regions that we're not already subscribed to
      (newRegions -- regions) foreach { region =>
        regionManager ! Subscribe(region, self)
      }
      // Unsubscribe from any regions that we no longer should be subscribed to
      (regions -- newRegions) foreach { region =>
        regionManager ! Unsubscribe(region, self)
      }
      regions = newRegions
      currentArea = Some(bbox)

    case p: UserPosition =>
      updates += (p.id -> p)

    case RegionPoints(regionId, points) =>
      updates ++= points.map(p => p.id -> p)

    case Tick =>
      publish(PositionSubscriberUpdate(currentArea, updates.values.toVector))
      updates = Map.empty

  }

}
