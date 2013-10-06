package backend

import scala.concurrent.duration._
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import scala.concurrent.duration.Deadline

object Region {
  case object Tick
}

class Region extends Actor {
  import Region._

  val regionId = self.path.name
  val mediator = DistributedPubSubExtension(context.system).mediator
  var activeUsers = Map.empty[String, Deadline]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)
  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case p @ UserPosition(userId, _, _) =>
      activeUsers += (userId -> (Deadline.now + 30.seconds))
      // publish new user position to subscribers
      mediator ! Publish(regionId, p)

    case Tick =>
      val obsolite = activeUsers.collect {
        case (userId, deadline) if deadline.isOverdue => userId
      }
      activeUsers --= obsolite
      val count = RegionCount(regionId, activeUsers.size)
      // propagate the count to the summary region via the manager
      context.parent ! count
      // publish count to subscribers
      mediator ! Publish(regionId, count)
      if (activeUsers.isEmpty)
        context.stop(self)

  }

}