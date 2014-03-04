package actors

import scala.collection.immutable.Seq
import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe
import models.backend._
import backend.Settings

object PositionSubscriber {

  def props(subscriber: ActorRef): Props = Props(new PositionSubscriber(subscriber))

  case class PositionSubscriberUpdate(area: Option[BoundingBox], updates: Seq[PointOfInterest])

  private case object Tick
}

/**
 * A subscriber to position data.
 */
class PositionSubscriber(subscriber: ActorRef) extends Actor with ActorLogging {
  import PositionSubscriber._

  val mediator = DistributedPubSubExtension(context.system).mediator
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
        mediator ! Subscribe(region.name, self)
      }
      // Unsubscribe from any regions that we no longer should be subscribed to
      (regions -- newRegions) foreach { region =>
        mediator ! Unsubscribe(region.name, self)
      }
      regions = newRegions
      currentArea = Some(bbox)

    case p: UserPosition =>
      updates += (p.id -> p)

    case RegionPoints(regionId, points) =>
      updates ++= points.map(p => p.id -> p)

    case Tick =>
      subscriber ! PositionSubscriberUpdate(currentArea, updates.values.toVector)
      updates = Map.empty

  }

}
