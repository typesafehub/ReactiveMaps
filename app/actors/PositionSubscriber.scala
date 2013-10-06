package actors

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe
import backend.Area
import backend.RegionCount
import backend.UserPosition

object PositionSubscriber {
  case object Tick
}

class PositionSubscriber(publish: UserPosition => Unit) extends Actor with ActorLogging {
  import PositionSubscriber._

  val mediator = DistributedPubSubExtension(context.system).mediator
  var regions = Set.empty[String]
  var updates: Map[String, UserPosition] = Map.empty

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)

  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case a: Area =>
      val newRegions = subscriptionRegions(a)
      (newRegions -- regions) foreach {
        mediator ! Subscribe(_, self)
      }
      (regions -- newRegions) foreach {
        mediator ! Unsubscribe(_, self)
      }
      regions = newRegions

    case p: UserPosition =>
      updates += (p.userId -> p)

    case RegionCount(regionId, count) =>
      // FIXME this should also be published to the channel
      log.info("Region {} has {} inhabitants", regionId, count)

    case Tick =>
      updates.foreach { case (_, p) => publish(p) }
      updates = Map.empty

  }

  // FIXME from the Area we must compute overlapping regions, if too many regions we
  // go up to appropriate summary level.
  def subscriptionRegions(area: Area): Set[String] = Set("dummyRegion1", "dummyRegion2")

}