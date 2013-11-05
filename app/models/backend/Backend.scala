package models.backend

import scala.collection.immutable.IndexedSeq
import play.extras.geojson.LatLng

/**
 * A point of interest, either a user position or a cluster of positions
 */
sealed trait PointOfInterest {
  /**
   * The id of the point of interest
   */
  def id: String
  /**
   * When the point of interest was created
   */
  def timestamp: Long
  /**
   * The position of the point of interest
   */
  def position: LatLng
}

/**
 * A user position
 */
case class UserPosition(id: String, timestamp: Long, position: LatLng) extends PointOfInterest

/**
 * A cluster of user positions
 */
case class Cluster(id: String, timestamp: Long, position: LatLng, count: Long) extends PointOfInterest

/**
 * @param southWest The south western most point
 * @param northEast The north eastern most point
 */
case class BoundingBox(southWest: LatLng, northEast: LatLng) {
  require(southWest.lat < northEast.lat, "South west bound point is north of north east point")
}

/**
 * The points of interest for a given regionId.
 */
case class RegionPoints(regionId: RegionId, points: IndexedSeq[PointOfInterest])

/**
 * A region id.
 *
 * The zoomLevel indicates how deep this region is zoomed, a zoom level of 8 means that there are 2 ^^ 8 steps on the
 * axis of this zoomLevel, meaning the zoomLevel contains a total of 2 ^^ 16 regions.
 *
 * The x value starts at 0 at -180 West, and goes to 2 ^^ zoomLevel at 180 East.  The y value starts at 0 at -90 South,
 * and goes to 2 ^^ zoomLevel at 90 North.
 */
case class RegionId(zoomLevel: Int, x: Int, y: Int) {
  val name = s"region-$zoomLevel-$x-$y"
}
