package actors

import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.contrib.pattern.DistributedPubSubMediator.Unsubscribe

class PositionSubscriber(publish: UserPosition => Unit) extends Actor {

  val mediator = DistributedPubSubExtension(context.system).mediator
  var regions = Set.empty[String]

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

    case p: UserPosition => publish(p)

  }

  def subscriptionRegions(area: Area): Set[String] = Set("dummyRegion1", "dummyRegion2")

}