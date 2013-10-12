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
case class RegionPoints(regionId: RegionId, points: Seq[PointOfInterest])

case class RegionId(zoomLevel: Int, x: Int, y: Int) {
  def summaryRegionId: Option[RegionId] = {
    if (zoomLevel == 0) None
    else Some(GeoFunctions.summaryRegionForRegion(this))
  }
  def boundingBox: BoundingBox = {
    GeoFunctions.boundingBoxForRegion(this)
  }
  val name = "region-" + zoomLevel + "-" + x + "-" + y
}
object RegionId {
  def apply(pos: LatLng) = {
    GeoFunctions.regionForPoint(GeoFunctions.MaxDepth, pos)
  }
}
