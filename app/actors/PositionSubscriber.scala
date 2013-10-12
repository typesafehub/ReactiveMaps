package actors

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.contrib.pattern.DistributedPubSubExtension
import backend._
import backend.RegionPoints
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe
import backend.BoundingBox
import backend.UserPosition

object PositionSubscriber {
  case object Tick
  case class PositionSubscriberUpdate(area: Option[BoundingBox], updates: Seq[PointOfInterest])
}

import PositionSubscriber.PositionSubscriberUpdate

class PositionSubscriber(publish: PositionSubscriberUpdate => Unit) extends Actor with ActorLogging {
  import PositionSubscriber._

  val mediator = DistributedPubSubExtension(context.system).mediator
  var regions = Set.empty[RegionId]
  var updates: Map[String, PointOfInterest] = Map.empty
  var currentArea: Option[BoundingBox] = None

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(2.seconds, 2.seconds, self, Tick)

  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case bbox: BoundingBox =>
      val newRegions = GeoFunctions.regionsForBoundingBox(bbox).toSet
      (newRegions -- regions) foreach { region =>
        mediator ! Subscribe(region.name, self)
      }
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
      publish(new PositionSubscriberUpdate(currentArea, updates.values.toSeq))
      updates = Map.empty

  }

}