package backend

import models.geojson.LatLng

sealed trait PointOfInterest {
  def id: String
  def timestamp: Long
  def position: LatLng
}

case class UserPosition(id: String, timestamp: Long, position: LatLng) extends PointOfInterest
case class Cluster(id: String, timestamp: Long, position: LatLng, count: Long) extends PointOfInterest

/**
 * @param southWest The south western most point
 * @param northEast The north eastern most point
 */
case class BoundingBox(southWest: LatLng, northEast: LatLng) {
  assert(southWest.lat < northEast.lat, "South west bound point is north of north east point")
}
case class RegionPoints(regionId: String, points: Seq[PointOfInterest])
