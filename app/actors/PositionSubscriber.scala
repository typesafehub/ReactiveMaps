package actors

import scala.concurrent.duration._
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe

object PositionSubscriber {
  case object Tick
}

class PositionSubscriber(publish: UserPosition => Unit) extends Actor {
  import PositionSubscriber._

  val mediator = DistributedPubSubExtension(context.system).mediator
  var regions = Set.empty[String]
  var updates = Map.empty[String, UserPosition]

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

    case Tick =>
      updates.foreach { case (_, p) => publish(p) }
      updates = Map.empty[String, UserPosition]

  }

  def subscriptionRegions(area: Area): Set[String] = Set("dummyRegion1", "dummyRegion2")

}