package backend

import scala.concurrent.duration._
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import scala.concurrent.duration.Deadline
import models.geojson.LatLng

object Region {
  case object Tick
}

class Region(regionId: RegionId) extends Actor {
  import Region._

  val regionBounds: BoundingBox = regionId.boundingBox
  val mediator = DistributedPubSubExtension(context.system).mediator
  var activeUsers = Map.empty[String, UserPosition]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)
  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case p @ UserPosition(userId, _, _) =>
      activeUsers += (userId -> p)
      // publish new user position to subscribers
      mediator ! Publish(regionId.name, p)

    case Tick =>
      val maxAge = System.currentTimeMillis() - 30.seconds.toMillis
      val obsolete = activeUsers.collect {
        case (userId, position) if maxAge > position.timestamp => userId
      }
      activeUsers --= obsolete

      // Cluster
      val points = RegionPoints(regionId, GeoFunctions.clusterNBoxes(regionId.name, regionBounds, 4, activeUsers.values.toSeq))

      // propagate the points to the summary region via the manager
      context.parent ! points
      // publish count to subscribers
      mediator ! Publish(regionId.name, points)
      if (activeUsers.isEmpty)
        context.stop(self)

  }

}