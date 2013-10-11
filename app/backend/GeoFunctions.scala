package backend

import models.geojson.LatLng

/**
 * Geo functions.
 */
object GeoFunctions {

  val MaxDepth = 10
  val MaxRegionsForBoundingBox = 3

  /**
   * Get the region for the given point.
   *
   * @param zoomDepth The zoom depth.
   * @param point The point.
   * @return The id of the region at the given zoom depth.
   */
  def regionForPoint(zoomDepth: Int, point: LatLng): RegionId = {
    assert(zoomDepth <= MaxDepth, "Too deep!")
    val axisSteps = 1l << zoomDepth
    val lngStep = 360d / axisSteps
    val lng = Math.floor((point.lng + 180) / lngStep).asInstanceOf[Long]
    val latStep = 180d / axisSteps
    val lat = Math.floor((point.lat + 90) / latStep).asInstanceOf[Long]
    RegionId(zoomDepth, lat * axisSteps + lng)
  }

  /**
   * Get the regions for the given bounding box.
   *
   * @param bbox The bounding box.
   * @return The regions
   */
  def regionsForBoundingBox(bbox: BoundingBox): Seq[RegionId] = {
    def regionsAtZoomLevel(zoomLevel: Int): Seq[RegionId] = {
      if (zoomLevel == 0) {
        Seq(RegionId(0, 0))
      } else {
        val axisSteps = 1l << zoomLevel
        val lngMask = axisSteps - 1
        val southWestRegion = regionForPoint(zoomLevel, bbox.southWest).id
        val northEastRegion = regionForPoint(zoomLevel, bbox.northEast).id
        val south = southWestRegion >>> zoomLevel
        val west = southWestRegion & lngMask
        val north = (northEastRegion >>> zoomLevel) + 1
        val east = (northEastRegion & lngMask) + 1
        val southNorth = north - south
        val westEast = east - west
        val numRegions = southNorth * westEast
        if (numRegions <= 0) {
          Seq(RegionId(0, 0))
        } else if (MaxRegionsForBoundingBox >= numRegions) {
          for (i <- 0l until numRegions) yield {
            val y = i / southNorth
            val x = i % southNorth
           RegionId(zoomLevel, (south + y) * axisSteps + west + x)
          }
        } else {
          regionsAtZoomLevel(zoomLevel - 1)
        }
      }
    }
    regionsAtZoomLevel(MaxDepth)
  }

  /**
   * Get the bounding box for the given region.
   */
  def boundingBoxForRegion(regionId: RegionId): BoundingBox = {
    val axisSteps = 1l << regionId.zoomLevel
    val lngMask = axisSteps - 1
    val latStep = 180d / axisSteps
    val lngStep = 360d / axisSteps
    val latRegion = (regionId.id >>> regionId.zoomLevel) * latStep - 90
    val lngRegion = (regionId.id & lngMask) * lngStep - 180

    BoundingBox(
      LatLng(latRegion, lngRegion),
      LatLng(latRegion + latStep, lngRegion + lngStep)
    )
  }

  def summaryRegionForRegion(regionId: RegionId): RegionId = {
    assert(regionId.zoomLevel != 0, "Can't summarize zoom level 0")
    // I'm sure there's a faster way to do this...
    val x = regionId.id & ((1l << regionId.zoomLevel) - 1)
    val y = regionId.id >>> regionId.zoomLevel
    val summarySteps = 1l << (regionId.zoomLevel - 1)
    RegionId(regionId.zoomLevel - 1, (y >>> 1) * summarySteps + (x >>> 1))
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
          val normalisedWest =  modPositive(next.position.lng + 180d, 360)
          next match {
            case u: UserPosition => (totals._1 + normalisedWest, totals._2 + next.position.lat, totals._3 + 1)
            case Cluster(_, _, _, c) => (totals._1 + normalisedWest * c, totals._2 + next.position.lat * c, totals._3 + c)
          }
        }
        Cluster(id + "-" + idx, System.currentTimeMillis(), LatLng(lat / count, (lng / count) - 180d), count)
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
