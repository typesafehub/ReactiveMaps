package backend

import models.geojson.LatLng

/**
 * Geo functions.
 *
 * We divide the earth into 2 ^^ 32 regions (an exponential divide of the earth into 4 boxes each time).  This is
 * at a zoom depth of 5. There are then 4 levels of summary regions above this, 2 ^^ 16, 2 ^^ 4, 2 ^^ 2, and 2 ^^ 0
 * being the whole earth.  Each region is identified by the zoom level and then the id, starting at 0 in the southwest,
 * and going east first.
 */
object GeoFunctions {

  /**
   * Get the region for the given point.
   *
   * @param zoomDepth The zoom depth.
   * @param point The point.
   * @return The id of the region at the given zoom depth.
   */
  def regionForPoint(zoomDepth: Int, point: LatLng): Long = {
    assert(zoomDepth <= 5, "Too deep!")
    val axisSize = 1l << zoomDepth
    val lngStep = 360d / axisSize
    val lng = Math.floor((point.lng + 180) / lngStep).asInstanceOf[Long]
    val latStep = 180d / axisSize
    val lat = Math.floor((point.lat + 90) / latStep).asInstanceOf[Long]
    lat * axisSize + lng
  }

  /**
   * Get the regions for the given bounding box.
   *
   * @param maxRegions The maximum number of regions to return.
   * @param bbox The bounding box.
   * @return The regions
   */
  def regionsForBoundingBox(maxRegions: Int, bbox: BoundingBox): (Int, Seq[Long]) = {
    def regionsAtZoomLevel(zoomLevel: Int): (Int, Seq[Long]) = {
      if (zoomLevel == 0) {
        (0, Seq(0))
      } else {
        val axisSize = 1l << zoomLevel
        val lngMask = (1l << zoomLevel) - 1
        val southWestRegion = regionForPoint(zoomLevel, bbox.southWest)
        val northEastRegion = regionForPoint(zoomLevel, bbox.northEast)
        val south = southWestRegion >>> zoomLevel
        val west = southWestRegion & lngMask
        val southNorth = northEastRegion / axisSize - south
        val westEast = northEastRegion % axisSize - west
        val numRegions = southNorth * westEast
        if (maxRegions >= numRegions) {
          (zoomLevel, for (i <- 0 until numRegions) yield {
            val y = i / southNorth
            val x = i % southNorth
            (south + y) * axisSize + west + x
          })
        } else {
          regionsAtZoomLevel(zoomLevel - 1)
        }
      }
    }
    regionsAtZoomLevel(5)
  }

  /**
   * Get the bounding box for the given region.
   */
  def boundingBoxForRegion(zoomLevel: Int, region: Long): BoundingBox = {
    val lngMask = (1l << zoomLevel) - 1
    val axisSize = 1l << zoomLevel
    val latStep = 180d / axisSize
    val lngStep = 360d / axisSize
    val latRegion = (region >>> zoomLevel) * latStep - 90
    val lngRegion = (region & lngMask) * lngStep - 180

    BoundingBox(
      LatLng(latRegion, lngRegion),
      LatLng(latRegion + latStep, lngRegion + lngStep)
    )
  }

  def summaryRegionForRegion(zoomLevel: Int, region: Long): Long = {
    assert(zoomLevel != 0, "Can't summarize zoom level 0")
    // I'm sure there's a faster way to do this...
    val x = region & ((1l << zoomLevel) - 1)
    val y = region >>> zoomLevel
    val summaryAxis = 1l << (zoomLevel - 1)
    (y >>> 1) * summaryAxis + (x >>> 1)
  }

  /**
   * Cluster the given points into n2 boxes
   *
   * @param id The id of the region
   * @param bbox The bounding box within which to cluster
   * @param points The points to cluster
   * @return The clustered points
   */
  def clusterNBoxes(id: String, bbox: BoundingBox, n: Int, points: Seq[PointOfInterest]): Seq[PointOfInterest] = {
    groupNBoxes(bbox, n, points).zipWithIndex.map {
      case (Seq(single), _) => single
      // The fold operation here normalises all points to making the west of the bounding box 0, and then takes an average
      case (multiple, idx) =>
        val (lng, lat, count) = multiple.foldLeft((0d, 0d, 0l)) { (totals, next) =>
          val normalisedWest =  modPositive(next.position.lng - bbox.southWest.lat, 360)
          next match {
            case u: UserPosition => (totals._1 + normalisedWest, totals._2 + next.position.lat, totals._3 + 1)
            case Cluster(_, _, _, c) => (totals._1 + normalisedWest * c, totals._2 + next.position.lat * c, totals._3 + c)
          }
        }
        Cluster(id + "-" + idx, System.currentTimeMillis(), LatLng(lat, lng), count)
    }
  }

  /**
   * Group the positions into n2 boxes
   *
   * @param bbox The bounding box
   * @param positions The positions to group
   * @return The grouped positions
   */
  def groupNBoxes(bbox: BoundingBox, n: Int, positions: Seq[PointOfInterest]): Seq[Seq[PointOfInterest]] = {
    positions.groupBy { pos =>
      latitudeSegment(n, bbox.southWest.lat, bbox.northEast.lat, pos.position.lat) * n +
        longitudeSegment(n, bbox.southWest.lng, bbox.northEast.lng, pos.position.lng)
    }.values.toSeq
  }

  /**
   * Find the segment that the point lies in in the given south/north range
   *
   * @return A number from 0 to n - 1
   */
  def latitudeSegment(n: Int, south: Double, north: Double, point: Double): Int = {
    // Normalise so that the southern most point is 0
    val range = north - south
    val normalisedPoint = point - south
    val segment = Math.floor(normalisedPoint * (n / range)).asInstanceOf[Int]
    if (segment >= n || segment < 0) {
      // The point was never in the given range.  Default to 0.
      0
    } else {
      segment
    }
  }

  /**
   * Find the segment that the point lies in in the given west/east range
   *
   * @return A number from 0 to n - 1
   */
  def longitudeSegment(n: Int, west: Double, east: Double, point: Double): Int = {
    // Normalise so that the western most point is 0, taking into account the 180 cut over
    val range = modPositive(east - west, 360)
    val normalisedPoint = modPositive(point - west, 360)
    val segment = Math.floor(normalisedPoint * (n / range)).asInstanceOf[Int]
    if (segment >= n || segment < 0) {
      // The point was never in the given range.  Default to 0.
      0
    } else {
      segment
    }
  }

  /**
   * Modulo function that always returns a positive number
   */
  def modPositive(x: Double, y: Int): Double = {
    val mod = x % y
    if (mod > 0) mod else mod + y
  }

}
