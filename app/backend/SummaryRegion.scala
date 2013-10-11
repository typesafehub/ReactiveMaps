package backend

import scala.concurrent.duration._
import scala.concurrent.duration.Deadline
import akka.actor.Actor
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import models.geojson.LatLng

object SummaryRegion {
  case object Tick
}

class SummaryRegion(regionId: RegionId) extends Actor {
  import SummaryRegion._

  val regionBounds: BoundingBox = regionId.boundingBox
  val mediator = DistributedPubSubExtension(context.system).mediator

  var activePoints = Map.empty[String, PointOfInterest]

  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)
  override def postStop(): Unit = tickTask.cancel()

  def receive = {
    case RegionPoints(id, points) =>
      activePoints ++= points.map(p => p.id -> p)

    case Tick =>
      val maxAge = System.currentTimeMillis() - 30.seconds.toMillis
      val obsolete = activePoints.collect {
        case (id, point) if point.timestamp < maxAge => id
      }
      if (obsolete.nonEmpty) {
        activePoints --= obsolete
      }

      // Cluster
      val points = RegionPoints(regionId, GeoFunctions.clusterNBoxes(regionId.name, regionBounds, 4, activePoints.values.toSeq))

      // propagate the points to higher level summary region via the manager
      context.parent ! points
      // publish total count to subscribers
      mediator ! Publish(regionId.name, points)
      if (activePoints.isEmpty)
        context.stop(self)
  }

}