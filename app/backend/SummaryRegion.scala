package backend

import scala.concurrent.duration._
import scala.concurrent.duration.Deadline
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish

object SummaryRegion {
  case object Tick
}

class SummaryRegion extends Actor {
  import SummaryRegion._

  val regionId = self.path.name
  val mediator = DistributedPubSubExtension(context.system).mediator
  // counts per region
  var counts = Map.empty[String, Long]
  // deadlines per region to be able to cleanup inactive/empty regions
  var deadlines = Map.empty[String, Deadline]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)
  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case RegionCount(id, count) =>
      counts += (id -> count)
      deadlines += (id -> (Deadline.now + 30.seconds))

    case Tick =>
      val obsolite = deadlines.collect {
        case (regionId, deadline) if deadline.isOverdue => regionId
      }
      if (obsolite.nonEmpty) {
        counts --= obsolite
        deadlines --= obsolite
      }

      val count = RegionCount(regionId, counts.values.sum)
      // propagate the count to higher level summary region via the manager
      context.parent ! count
      // publish total count to subscribers
      mediator ! Publish(regionId, count)
      if (counts.isEmpty)
        context.stop(self)
  }

}