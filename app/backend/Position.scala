package backend

import models.geojson.LatLng

case class UserPosition(userId: String, timestamp: Long, position: LatLng)
case class BoundingBox(a: LatLng, b: LatLng)
case class RegionCount(regionId: String, count: Long)